package tool.naucourse.ics.network.clients.tools

import okhttp3.ConnectionPool
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import tool.naucourse.ics.network.clients.utils.CookieStore
import tool.naucourse.ics.network.clients.utils.UAInterceptor
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit

object SSONetworkTools {
    @Volatile
    private var cookieStore: CookieStore? = null

    @Volatile
    private var okhttpClient: OkHttpClient? = null

    // TimeUnit.SECONDS
    private const val CONNECT_TIME_OUT = 20L
    // TimeUnit.SECONDS
    private const val READ_TIME_OUT = 10L
    // TimeUnit.SECONDS
    private const val WRITE_TIME_OUT = 10L

    private const val MAX_CONNECTION_NUM = 15

    // TimeUnit.MINUTES
    private const val ALIVE_CONNECTION_NUM = 5L

    fun getClient(): OkHttpClient = okhttpClient
        ?: synchronized(this) {
            okhttpClient
                ?: createClient(
                    cookieStore
                        ?: createCookieStore()
                ).also { okhttpClient = it }
        }

    fun getCookieStore(): CookieStore = cookieStore
        ?: synchronized(this) {
            cookieStore
                ?: createCookieStore().also { cookieStore = it }
        }

    fun getResponseContent(response: Response): String {
        val body = response.body
        val contentType = body?.contentType()
        val source = body?.source()
        source!!.request(Long.MAX_VALUE)
        val buffer = source.buffer

        val bufferClone = buffer.clone()
        val result = bufferClone.readString(
            (if (contentType != null) contentType.charset(StandardCharsets.UTF_8) else StandardCharsets.UTF_8)
                ?: StandardCharsets.UTF_8
        )
        bufferClone.close()
        return result
    }

    private fun createCookieStore(): CookieStore =
        CookieStore()

    private fun createClient(cookieStore: CookieStore): OkHttpClient {
        return OkHttpClient.Builder().apply {
            connectTimeout(CONNECT_TIME_OUT, TimeUnit.SECONDS)
            readTimeout(READ_TIME_OUT, TimeUnit.SECONDS)
            writeTimeout(WRITE_TIME_OUT, TimeUnit.SECONDS)
            retryOnConnectionFailure(true)
            cookieJar(cookieStore)
            followRedirects(true)
            followSslRedirects(true)
            connectionPool(
                ConnectionPool(
                    MAX_CONNECTION_NUM,
                    ALIVE_CONNECTION_NUM,
                    TimeUnit.MINUTES
                )
            )
            addInterceptor(UAInterceptor())
        }.build()
    }

    fun HttpUrl.hasSameHost(url: HttpUrl?): Boolean =
        url != null && this.host.toLowerCase(Locale.CHINA) == url.host.toLowerCase(Locale.CHINA)

    fun HttpUrl.hasSameHost(host: String?): Boolean =
        host != null && this.host.toLowerCase(Locale.CHINA) == host.toLowerCase(Locale.CHINA)

}