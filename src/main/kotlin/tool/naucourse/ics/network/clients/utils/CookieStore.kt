package tool.naucourse.ics.network.clients.utils

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class CookieStore : CookieJar {
    private val cookieMap: ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>> = ConcurrentHashMap()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        for (cookie in cookies) {
            if (cookie.persistent) {
                if (cookieMap.containsKey(host)) {
                    cookieMap[host]?.remove(cookie.name)
                }
            } else {
                if (!cookieMap.containsKey(host)) {
                    cookieMap[host] = ConcurrentHashMap(cookies.size)
                }
                cookieMap[host]?.put(cookie.name, cookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val result = ArrayList<Cookie>()
        val cookies = cookieMap[url.host]
        cookies?.forEach {
            result.add(it.value)
        }
        return result
    }

    fun clearCookies() {
        cookieMap.clear()
    }

    fun clearCookies(host: String) {
        cookieMap.remove(host)
    }
}
