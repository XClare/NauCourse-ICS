package tool.naucourse.ics.network

import tool.naucourse.ics.network.clients.JwcClient
import tool.naucourse.ics.network.clients.base.LoginInfo

object NauNetworkManager {
    @Volatile
    private lateinit var jwcClient: JwcClient

    private lateinit var loginInfo: LoginInfo

    fun getJwcClient(): JwcClient = synchronized(this) {
        if (!NauNetworkManager::jwcClient.isInitialized) {
            jwcClient =
                JwcClient(loginInfo)
        }
        return jwcClient
    }

    fun setLoginInfo(loginInfo: LoginInfo) {
        NauNetworkManager.loginInfo = loginInfo
    }
}