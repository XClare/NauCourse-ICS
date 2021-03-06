package tool.naucourse.ics.network.clients

import okhttp3.*
import okhttp3.internal.closeQuietly
import org.jsoup.Jsoup
import tool.naucourse.ics.Constants
import tool.naucourse.ics.network.clients.base.BaseLoginClient
import tool.naucourse.ics.network.clients.base.LoginInfo
import tool.naucourse.ics.network.clients.base.LoginResponse
import tool.naucourse.ics.network.clients.tools.SSONetworkTools
import tool.naucourse.ics.network.clients.tools.SSONetworkTools.hasSameHost
import java.io.IOException

open class SSOClient(private val loginInfo: LoginInfo, private val serviceUrl: HttpUrl? = null) :
    BaseLoginClient(loginInfo) {
    private val okHttpClient = SSONetworkTools.getClient()
    private val cookieStore = SSONetworkTools.getCookieStore()
    private val isServiceLogin = serviceUrl != null

    private val loginUrl: HttpUrl

    init {
        val loginUrlBuilder = SSO_LOGIN_URL.newBuilder()
        if (serviceUrl != null) {
            loginUrlBuilder.addQueryParameter(SSO_SERVICE_PARAM, serviceUrl.toString())
        }
        loginUrl = loginUrlBuilder.build()
    }

    companion object {
        private const val SSO_HOST = "sso.nau.edu.cn"
        private const val SSO_SERVICE_PARAM = "service"
        private const val SSO_PATH = "sso"
        private const val SSO_LOGIN_PATH = "login"
        private const val SSO_LOGOUT_PATH = "logout"

        private val SSO_LOGIN_URL =
            HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
                SSO_HOST
            ).addPathSegment(SSO_PATH)
                .addEncodedPathSegment(SSO_LOGIN_PATH).build()
        private val SSO_LOGOUT_URL =
            HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
                SSO_HOST
            ).addPathSegment(SSO_PATH)
                .addEncodedPathSegment(SSO_LOGOUT_PATH).build()

        private val SSO_LOGIN_PARAM =
            arrayOf("lt", "execution", "_eventId", "useVCode", "isUseVCode", "sessionVcode", "errorCount")
        private const val SSO_INPUT_TAG_NAME_ATTR = "name"
        private const val SSO_INPUT_TAG_VALUE_ATTR = "value"
        private const val SSO_INPUT = "input[$SSO_INPUT_TAG_NAME_ATTR][$SSO_INPUT_TAG_VALUE_ATTR]"
        private const val SSO_POST_FORMAT = "input[$SSO_INPUT_TAG_NAME_ATTR=%s]"
        private const val SSO_POST_USERNAME = "username"
        private const val SSO_POST_PASSWORD = "password"

        private const val SSO_LOGIN_SUCCESS_STR = "登录成功"
        private const val SSO_LOGOUT_SUCCESS_STR = "注销成功"
        private const val SSO_LOGIN_PASSWORD_ERROR_STR = "密码错误"
        private const val SSO_LOGIN_INPUT_ERROR_STR = "请勿输入非法字符"
        const val SSO_LOGIN_PAGE_STR = "南京审计大学统一身份认证登录"

        private val SSO_HEADER = Headers.headersOf(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3",
            "Accept-Language",
            "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7",
            "Connection",
            "keep-alive",
            "Cache-Control",
            "max-age=0",
            "Upgrade-Insecure-Requests",
            "1"
        )

        private fun getSSOLoginStatus(htmlContent: String): LoginResponse.ErrorResult = when {
            SSO_LOGIN_SUCCESS_STR in htmlContent -> LoginResponse.ErrorResult.NONE
            SSO_LOGIN_PASSWORD_ERROR_STR in htmlContent -> LoginResponse.ErrorResult.PASSWORD_ERROR
            SSO_LOGIN_INPUT_ERROR_STR in htmlContent -> LoginResponse.ErrorResult.INPUT_ERROR
            else -> LoginResponse.ErrorResult.UNKNOWN
        }

        private fun getLoginPostForm(userId: String, userPw: String, ssoResponseContent: String): FormBody =
            FormBody.Builder().apply {
                add(SSO_POST_USERNAME, userId)
                add(SSO_POST_PASSWORD, userPw)
                val htmlContent = Jsoup.parse(ssoResponseContent).select(SSO_INPUT)
                for (param in SSO_LOGIN_PARAM) {
                    val input = htmlContent.select(SSO_POST_FORMAT.format(param)).first()
                    add(param, input.attr(SSO_INPUT_TAG_VALUE_ATTR))
                }
            }.build()
    }

    override fun getNetworkClient(): OkHttpClient = okHttpClient

    @Synchronized
    final override fun login(ssoResponse: Response): LoginResponse {
        val ssoResponseUrl = ssoResponse.request.url
        val ssoResponseContent = ssoResponse.body?.string()!!
        ssoResponse.closeQuietly()
        when {
            SSO_LOGIN_PAGE_STR in ssoResponseContent || ssoResponseUrl.hasSameHost(
                SSO_HOST
            ) -> {
                if (!isServiceLogin && getSSOLoginStatus(
                        ssoResponseContent
                    ) == LoginResponse.ErrorResult.NONE
                ) {
                    return LoginResponse(
                        true,
                        ssoResponseUrl,
                        ssoResponseContent
                    )
                }
                val postForm =
                    getLoginPostForm(
                        loginInfo.userId,
                        loginInfo.userPw,
                        ssoResponseContent
                    )
                val request = ssoResponse.request.newBuilder().apply {
                    post(postForm)
                }.build()
                newSSOCall(request).use {
                    if (it.isSuccessful) {
                        val url = it.request.url
                        val content = it.body?.string()!!
                        return if (isServiceLogin) {
                            when {
                                url.hasSameHost(serviceUrl) -> {
                                    LoginResponse(
                                        true,
                                        url,
                                        content
                                    )
                                }
                                url.hasSameHost(SSO_HOST) -> {
                                    LoginResponse(
                                        false,
                                        loginErrorResult = getSSOLoginStatus(
                                            content
                                        )
                                    )
                                }
                                else -> {
                                    LoginResponse(
                                        false,
                                        loginErrorResult = LoginResponse.ErrorResult.SERVER_ERROR
                                    )
                                }
                            }
                        } else {
                            val status =
                                getSSOLoginStatus(
                                    content
                                )
                            if (status == LoginResponse.ErrorResult.NONE) {
                                LoginResponse(
                                    true,
                                    url,
                                    content
                                )
                            } else {
                                LoginResponse(
                                    false,
                                    loginErrorResult = getSSOLoginStatus(
                                        content
                                    )
                                )
                            }
                        }
                    } else {
                        throw IOException("SSO Login Failed!")
                    }
                }
            }
            ssoResponseUrl.hasSameHost(serviceUrl) -> return LoginResponse(
                true,
                ssoResponseUrl,
                ssoResponseContent
            )
            else -> throw IOException("SSO Jump Url Error! $ssoResponseUrl")
        }
    }

    override fun logout(): Boolean = ssoLogout()

    fun ssoLogout(clearCookies: Boolean = true): Boolean =
        newSSOCall(Request.Builder().url(SSO_LOGOUT_URL).build()).use {
            val result = it.body?.string()!!.contains(SSO_LOGOUT_SUCCESS_STR)
            if (clearCookies) {
                cookieStore.clearCookies()
            }
            result
        }

    override fun validateLoginWithResponse(responseContent: String, responseUrl: HttpUrl): Boolean =
        if (isServiceLogin) {
            responseUrl.hasSameHost(serviceUrl)
        } else {
            getSSOLoginStatus(responseContent) == LoginResponse.ErrorResult.NONE
        }

    override fun newClientCall(request: Request): Response = newSSOCall(request)

    private fun newSSOCall(request: Request): Response =
        okHttpClient.newCall(request.newBuilder().headers(SSO_HEADER).build()).execute()

    final override fun getBeforeLoginResponse(): Response {
        val request = Request.Builder().apply {
            url(loginUrl)
        }.build()
        return newSSOCall(request)
    }

    final override fun newAutoLoginCall(request: Request): Response {
        val response = newClientCall(request)
        val url = response.request.url
        val content = SSONetworkTools.getResponseContent(response)
        return if (validateLoginWithResponse(content, url)) {
            response
        } else {
            val result = if (url.hasSameHost(SSO_HOST) || SSO_LOGIN_PAGE_STR in content) {
                login(response)
            } else {
                login()
            }
            if (result.isSuccess) {
                response.closeQuietly()
                newClientCall(request)
            } else {
                response
            }
        }
    }
}