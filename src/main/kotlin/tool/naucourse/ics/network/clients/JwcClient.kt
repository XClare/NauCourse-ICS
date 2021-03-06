package tool.naucourse.ics.network.clients

import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import tool.naucourse.ics.Constants
import tool.naucourse.ics.network.clients.base.LoginInfo
import tool.naucourse.ics.network.clients.base.LoginResponse

class JwcClient(loginInfo: LoginInfo) : SSOClient(
    loginInfo,
    JWC_SSO_LOGIN_URL
) {
    @Volatile
    private var jwcMainUrl: HttpUrl? = null

    companion object {
        const val JWC_HOST = "jwc.nau.edu.cn"
        private const val JWC_LOGIN_ASPX = "login.aspx"
        private const val JWC_DEFAULT_ASPX = "default.aspx"
        private const val JWC_LOGOUT_ASPX = "LoginOut.aspx"
        private const val JWC_SSO_LOGIN_ASPX = "Login_Single.aspx"
        const val JWC_STUDENTS_PATH = "Students"
        private const val JWC_URL_PARAM_R = "r"
        private const val JWC_URL_PARAM_D = "d"

        private val JWC_LOGIN_URL =
            HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
                JWC_HOST
            ).addPathSegment(JWC_LOGIN_ASPX).build()
        private val JWC_LOGOUT_URL =
            HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
                JWC_HOST
            ).addPathSegment(JWC_LOGOUT_ASPX).build()
        private val JWC_SSO_LOGIN_URL =
            HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
                JWC_HOST
            ).addPathSegment(JWC_SSO_LOGIN_ASPX).build()

        private const val JWC_ALREADY_LOGIN_STR = "已经登录"
        private const val JWC_SERVER_ERROR_STR = "非法字符"
        private const val JWC_PASSWORD_ERROR_STR = "密码错误"
        private const val JWC_LOGIN_PAGE_STR = "用户登录"

        private val JWC_HEADER = Headers.headersOf(
            "Accept", "*/*",
            "Accept-Language", "zh-CN,zh;q=0.9",
            "Connection", "keep-alive",
            "Content-Type", "application/x-www-form-urlencoded; charset=UTF-8",
            "X-Requested-With", "XMLHttpRequest;"
        )

        private fun getJwcLoginStatus(htmlContent: String): LoginResponse.ErrorResult = when {
            JWC_PASSWORD_ERROR_STR in htmlContent -> LoginResponse.ErrorResult.PASSWORD_ERROR
            JWC_SERVER_ERROR_STR in htmlContent -> LoginResponse.ErrorResult.SERVER_ERROR
            JWC_LOGIN_PAGE_STR in htmlContent || JWC_ALREADY_LOGIN_STR in htmlContent -> LoginResponse.ErrorResult.ALREADY_LOGIN
            else -> LoginResponse.ErrorResult.NONE
        }

        private fun validateJwcLoginUrl(url: HttpUrl): Boolean =
            url.pathSegments.size >= 2 && url.pathSegments[0] == JWC_STUDENTS_PATH && url.pathSegments[1] == JWC_DEFAULT_ASPX &&
                    url.querySize >= 2 && url.queryParameter(JWC_URL_PARAM_D) != null && url.queryParameter(
                JWC_URL_PARAM_R
            ) != null
    }

    override fun login(): LoginResponse {
        return jwcLogin()
    }

    private fun jwcLogin(loginOnce: Boolean = false): LoginResponse {
        val ssoResult = super.login()
        if (ssoResult.isSuccess) {
            val status =
                getJwcLoginStatus(ssoResult.htmlContent!!)
            return if (status == LoginResponse.ErrorResult.NONE && validateJwcLoginUrl(
                    ssoResult.url!!
                )
            ) {
                jwcMainUrl = ssoResult.url
                ssoResult
            } else if (!loginOnce && status == LoginResponse.ErrorResult.ALREADY_LOGIN) {
                if (jwcLogout()) {
                    jwcLogin(true)
                } else {
                    LoginResponse(
                        false,
                        loginErrorResult = LoginResponse.ErrorResult.ALREADY_LOGIN
                    )
                }
            } else {
                LoginResponse(
                    false,
                    loginErrorResult = status
                )
            }
        }
        return ssoResult
    }

    override fun logout(): Boolean = jwcLogout() && super.logout()

    fun jwcLogout(): Boolean = newJwcCall(Request.Builder().url(JWC_LOGOUT_URL).build()).use {
        return it.isSuccessful && it.request.url == JWC_LOGIN_URL
    }

    override fun validateLoginWithResponse(responseContent: String, responseUrl: HttpUrl): Boolean {
        return super.validateLoginWithResponse(responseContent, responseUrl) &&
                getJwcLoginStatus(
                    responseContent
                ) == LoginResponse.ErrorResult.NONE
    }

    fun requestJwcMainContent(): Response {
        return if (jwcMainUrl == null) {
            val result = login()
            if (result.isSuccess) {
                newAutoLoginCall(jwcMainUrl!!)
            } else {
                throw IllegalStateException("Client Login Failed! Reason: ${result.loginErrorResult}")
            }
        } else {
            newAutoLoginCall(jwcMainUrl!!)
        }
    }

    private fun newJwcCall(request: Request): Response =
        getNetworkClient().newCall(request.newBuilder().headers(JWC_HEADER).build()).execute()

    override fun newClientCall(request: Request): Response = newJwcCall(request)
}