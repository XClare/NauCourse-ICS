package tool.naucourse.ics.network.clients.base

import okhttp3.HttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

abstract class BaseLoginClient(private var loginInfo: LoginInfo) : BaseNetworkClient() {

    fun setLoginInfo(loginInfo: LoginInfo) {
        this.loginInfo = loginInfo
    }

    abstract fun getBeforeLoginResponse(): Response

    @Synchronized
    open fun login(): LoginResponse {
        getBeforeLoginResponse().use {
            return if (it.isSuccessful) {
                login(it)
            } else {
                throw IOException("Request Server Error!")
            }
        }
    }

    abstract fun login(ssoResponse: Response): LoginResponse

    abstract fun logout(): Boolean

    abstract fun validateLoginWithResponse(responseContent: String, responseUrl: HttpUrl): Boolean

    abstract fun newAutoLoginCall(request: Request): Response

    fun newAutoLoginCall(url: HttpUrl): Response = newAutoLoginCall(Request.Builder().url(url).build())
}