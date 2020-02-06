package tool.naucourse.ics.contents.methods

import okhttp3.Response
import org.jsoup.Jsoup
import tool.naucourse.ics.Constants
import tool.naucourse.ics.contents.base.BaseContent
import tool.naucourse.ics.contents.beans.TermDate
import tool.naucourse.ics.network.NauNetworkManager


object TermInfo : BaseContent<TermDate>() {
    private val jwcClient = NauNetworkManager.getJwcClient()

    private const val ELEMENT_ID_TERM_INFO = "TermInfo"

    private const val IN_VACATION_WEEK_NUM = 0
    private const val IN_VACATION_STR = "放假中"

    override fun onRequestData(): Response = jwcClient.requestJwcMainContent()

    override fun onParseData(content: String): TermDate {
        val document = Jsoup.parse(content)
        val spanElements =
            document.body().getElementById(ELEMENT_ID_TERM_INFO).getElementsByTag(
                Constants.HTML.ELEMENT_TAG_SPAN
            )

        val weekText = spanElements[2].text().trim()
        val currentWeek = if (IN_VACATION_STR == weekText) {
            IN_VACATION_WEEK_NUM
        } else {
            weekText.substring(1, weekText.length - 1).toInt()
        }
        val startDate = Constants.Time.DATE_FORMAT_YMD.parse(spanElements[3].text())!!
        val endDate = Constants.Time.DATE_FORMAT_YMD.parse(spanElements[4].text())!!

        return TermDate(currentWeek, startDate, endDate)
    }
}