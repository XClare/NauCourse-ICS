package tool.naucourse.ics.contents.methods

import okhttp3.HttpUrl
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import tool.naucourse.ics.Constants
import tool.naucourse.ics.contents.base.BaseContent
import tool.naucourse.ics.contents.beans.Course
import tool.naucourse.ics.contents.beans.CourseSet
import tool.naucourse.ics.contents.beans.CourseTime
import tool.naucourse.ics.contents.beans.Term
import tool.naucourse.ics.network.NauNetworkManager
import tool.naucourse.ics.network.clients.JwcClient
import java.io.IOException

object MyCourseScheduleTable : BaseContent<CourseSet>() {
    private val jwcClient = NauNetworkManager.getJwcClient()

    private const val COURSE_TABLE_ASPX = "MyCourseScheduleTable.aspx"
    private val COURSE_TABLE_URL = HttpUrl.Builder().scheme(Constants.Network.HTTP).host(
        JwcClient.JWC_HOST
    )
        .addPathSegment(JwcClient.JWC_STUDENTS_PATH).addPathSegment(
            COURSE_TABLE_ASPX
        ).build()

    private const val STUDY_CHAR = '学'
    private const val ACADEMIC_YEAR_JOIN_SYMBOL = '-'
    private const val TIMES_CHAR = '第'
    private const val FIRST_CHAR = '一'
    private const val SECOND_CHAR = '二'
    private const val COURSE_LOCATION_STR = "上课地点："
    private const val COURSE_TIME_STR = "上课时间："
    private const val COURSE_TIME_JOIN_SYMBOL = " "
    private const val COURSE_NUM_CHAR = '节'
    private const val WEEK_NUM_CHAR = '周'
    private const val WEEK_TYPE_SINGLE_CHAR = '单'
    private const val WEEK_TYPE_DOUBLE_CHAR = '双'
    private const val WEEK_TYPE_AND_CHAR = '之'
    private const val MULTI_TIME_JOIN_SYMBOL = ','
    private const val FROM_TO_TIME_JOIN_SYMBOL = '-'

    override fun onRequestData(): Response = jwcClient.newAutoLoginCall(
        COURSE_TABLE_URL
    )

    override fun onParseData(content: String): CourseSet {
        val document = Jsoup.parse(content)
        val term = getTerm(document)
        val courseSet =
            getCourseSet(document)
        return CourseSet(courseSet, term)
    }

    private fun getTerm(document: Document): Term {
        val termElement = document.body().getElementsByClass(Constants.HTML.ELEMENT_CLASS_TD_TITLE).first()
        val text = termElement.text().trim()

        var startYear: Int? = null
        var endYear: Int? = null
        var termNum: Short? = null
        var nextTerm = false

        val strStuck = StringBuilder()
        for (c in text) {
            if (nextTerm) {
                if (c == FIRST_CHAR) {
                    termNum = 1
                } else if (c == SECOND_CHAR) {
                    termNum = 2
                }
                break
            } else if (c == ACADEMIC_YEAR_JOIN_SYMBOL) {
                startYear = strStuck.toString().toInt()
                strStuck.clear()
                continue
            } else if (c == STUDY_CHAR) {
                endYear = strStuck.toString().toInt()
                strStuck.clear()
                continue
            } else if (c == TIMES_CHAR) {
                nextTerm = true
                continue
            }
            strStuck.append(c)
        }
        strStuck.clear()
        return Term(startYear!!, endYear!!, termNum!!)
    }

    private fun getCourseSet(document: Document): Set<Course> {
        val contentElement = document.getElementById(Constants.HTML.ELEMENT_ID_CONTENT)
        val trElements = contentElement.getElementsByTag(Constants.HTML.ELEMENT_TAG_TR)

        val courseSet = HashSet<Course>(trElements.size)

        var id: String? = null
        var name: String? = null
        var teacher: String? = null
        var courseClass: String? = null
        var teachClass: String? = null
        var credit: Float? = null
        var type: String? = null
        var timeSet: Set<CourseTime>? = null

        for (tr in 1 until trElements.size) {
            val tdElements = trElements[tr].getElementsByTag(Constants.HTML.ELEMENT_TAG_TD)

            if (tdElements.size < 8) {
                throw IOException("Incomplete Course Data!")
            }

            for (td in 1 until tdElements.size) {
                when (td) {
                    1 -> id = tdElements[td].text()
                    2 -> name = tdElements[td].text()
                    3 -> teachClass = tdElements[td].text()
                    4 -> credit = tdElements[td].text().toFloat()
                    5 -> courseClass = tdElements[td].text()
                    6 -> type = tdElements[td].text()
                    7 -> teacher = tdElements[td].text()
                    8 -> timeSet =
                        getCourseTimeSet(
                            id!!,
                            tdElements[td]
                        )
                }
            }

            courseSet.add(
                Course(
                    id!!,
                    name!!,
                    teacher!!,
                    courseClass!!,
                    teachClass!!,
                    credit!!,
                    type!!,
                    timeSet!!
                )
            )
        }

        return courseSet
    }

    private fun getCourseTimeSet(courseId: String, element: Element): Set<CourseTime> {
        val text = element.text().trim()

        val courseLocationSplit = text.split(COURSE_LOCATION_STR).filter {
            it.isNotEmpty()
        }

        val courseTimeSet = HashSet<CourseTime>(courseLocationSplit.size)

        var location: String
        var weeks: ArrayList<CourseTime.Time>
        var weeksMode: CourseTime.WeeksMode
        var weekDay: Short
        var courseNum: ArrayList<CourseTime.Time>

        courseLocationSplit.forEach {
            weeks = ArrayList()
            courseNum = ArrayList()

            val courseTimeSplit = it.split(COURSE_TIME_STR)
            location = courseTimeSplit[0].trimEnd()
            val timeSplit = courseTimeSplit[1].split(COURSE_TIME_JOIN_SYMBOL)

            weeksMode = CourseTime.WeeksMode.PLAIN
            if (timeSplit[0].startsWith(TIMES_CHAR)) {
                val weekNumStr = timeSplit[0].substring(1, timeSplit[0].indexOf(WEEK_NUM_CHAR))
                if (MULTI_TIME_JOIN_SYMBOL in weekNumStr) {
                    weekNumStr.split(MULTI_TIME_JOIN_SYMBOL).forEach { num ->
                        if (FROM_TO_TIME_JOIN_SYMBOL in num) {
                            val temp = num.split(FROM_TO_TIME_JOIN_SYMBOL)
                            weeks.add(CourseTime.Time(temp[0].toInt(), temp[1].toInt()))
                        } else {
                            weeks.add(CourseTime.Time(num.toInt()))
                        }
                    }
                } else {
                    if (FROM_TO_TIME_JOIN_SYMBOL in weekNumStr) {
                        val temp = weekNumStr.split(FROM_TO_TIME_JOIN_SYMBOL)
                        weeks.add(CourseTime.Time(temp[0].toInt(), temp[1].toInt()))
                    } else {
                        weeks.add(CourseTime.Time(weekNumStr.toInt()))
                    }
                }
            } else if (WEEK_TYPE_AND_CHAR in timeSplit[0]) {
                val weekNum = timeSplit[0].subSequence(0, timeSplit[0].indexOf(WEEK_TYPE_AND_CHAR))
                    .split(FROM_TO_TIME_JOIN_SYMBOL)
                if (WEEK_TYPE_SINGLE_CHAR in timeSplit[0]) {
                    weeksMode = CourseTime.WeeksMode.SINGLE
                } else if (WEEK_TYPE_DOUBLE_CHAR in timeSplit[0]) {
                    weeksMode = CourseTime.WeeksMode.DOUBLE
                }
                weeks.add(CourseTime.Time(weekNum[0].toInt(), weekNum[1].toInt()))
            } else {
                val weekNum =
                    timeSplit[0].subSequence(0, timeSplit[0].indexOf(WEEK_NUM_CHAR)).split(
                        FROM_TO_TIME_JOIN_SYMBOL
                    )
                weeks.add(CourseTime.Time(weekNum[0].toInt(), weekNum[1].toInt()))
            }

            weekDay = timeSplit[2].toShort()

            val courseNumText = timeSplit[4].substring(0, timeSplit[4].indexOf(COURSE_NUM_CHAR))
            when {
                MULTI_TIME_JOIN_SYMBOL in courseNumText -> courseNumText.split(
                    MULTI_TIME_JOIN_SYMBOL
                ).forEach { num ->
                    courseNum.add(CourseTime.Time(num.toInt()))
                }
                FROM_TO_TIME_JOIN_SYMBOL in courseNumText -> {
                    val splitTemp = courseNumText.split(FROM_TO_TIME_JOIN_SYMBOL)
                    courseNum.add(CourseTime.Time(splitTemp[0].toInt(), splitTemp[1].toInt()))
                }
                else -> courseNum.add(CourseTime.Time(courseNumText.toInt()))
            }

            courseTimeSet.add(
                CourseTime(
                    courseId,
                    location,
                    weeksMode,
                    weeks.toTypedArray(),
                    weekDay,
                    courseNum.toTypedArray()
                )
            )
        }

        return courseTimeSet
    }
}