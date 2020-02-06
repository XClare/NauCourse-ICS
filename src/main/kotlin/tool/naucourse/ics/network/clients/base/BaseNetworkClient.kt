package tool.naucourse.ics.network.clients.base

import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

abstract class BaseNetworkClient {

    abstract fun getNetworkClient(): OkHttpClient

    abstract fun newClientCall(request: Request): Response

    fun newClientCall(url: HttpUrl) = newClientCall(Request.Builder().url(url).build())
}