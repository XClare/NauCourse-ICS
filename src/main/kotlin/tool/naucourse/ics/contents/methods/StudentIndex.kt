package tool.naucourse.ics.contents.methods

import okhttp3.HttpUrl
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import tool.naucourse.ics.Constants
import tool.naucourse.ics.contents.base.BaseContent
import tool.naucourse.ics.contents.beans.StudentPersonalInfo
import tool.naucourse.ics.network.NauNetworkManager
import tool.naucourse.ics.network.clients.JwcClient

object StudentIndex : BaseContent<StudentPersonalInfo>() {
    private val jwcClient = NauNetworkManager.getJwcClient()

    private const val JWC_STUDENT_INDEX_ASPX = "StudentIndex.aspx"

    private val JWC_STUDENT_INDEX_URL = HttpUrl.Builder().scheme(Constants.Network.HTTP).host(JwcClient.JWC_HOST)
        .addPathSegment(JwcClient.JWC_STUDENTS_PATH).addPathSegment(JWC_STUDENT_INDEX_ASPX).build()

    override fun onRequestData(): Response = jwcClient.newAutoLoginCall(JWC_STUDENT_INDEX_URL)

    override fun onParseData(content: String): StudentPersonalInfo {
        val document = Jsoup.parse(content)
        val bodyElement = document.body()
        return getPersonalInfo(bodyElement)
    }

    private fun getPersonalInfo(bodyElement: Element): StudentPersonalInfo {
        val tableElements = bodyElement.getElementsByTag(Constants.HTML.ELEMENT_TAG_TABLE).first()
        val trElements = tableElements.getElementsByTag(Constants.HTML.ELEMENT_TAG_TR)

        var stuId: Pair<String, String>? = null
        var name: Pair<String, String>? = null
        var grade: Pair<String, String>? = null
        var college: Pair<String, String>? = null
        var major: Pair<String, String>? = null
        var majorDirection: Pair<String, String>? = null
        var trainingDirection: Pair<String, String>? = null
        var currentClass: Pair<String, String>? = null

        for ((index, trElement) in trElements.withIndex()) {
            val tdElements = trElement.getElementsByTag(Constants.HTML.ELEMENT_TAG_TD)
            val pair = Pair(tdElements[0].text(), tdElements[1].text().trim())
            when (index) {
                0 -> stuId = pair
                1 -> name = pair
                2 -> grade = pair
                3 -> college = pair
                4 -> major = pair
                5 -> majorDirection = pair
                6 -> trainingDirection = pair
                7 -> currentClass = pair
            }
        }

        return StudentPersonalInfo(
            stuId!!,
            name!!,
            grade!!,
            college!!,
            major!!,
            majorDirection!!,
            trainingDirection!!,
            currentClass!!
        )
    }
}