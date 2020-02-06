package tool.naucourse.ics

import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val APPLICATION_NAME = "NauCourse-ICS"
    const val AUTHOR = "XClare"
    const val VERSION = "1.0.2"

    const val SPACE = " "

    object Network {
        const val HTTP = "http"
    }

    object HTML {
        const val ELEMENT_TAG_TD = "td"
        const val ELEMENT_TAG_TR = "tr"
        const val ELEMENT_TAG_SPAN = "span"
        const val ELEMENT_TAG_TABLE = "table"

        const val ELEMENT_CLASS_TD_TITLE = "tdTitle"

        const val ELEMENT_ID_CONTENT = "content"
    }

    object Time {
        const val MIN_WEEK_DAY = 0
        const val MAX_WEEK_DAY = 6
        val DATE_FORMAT_YMD = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val DATE_FORMAT_YMD_HM_CH = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINA)
    }

    object Course {
        const val MAX_WEEKS_SIZE = 24
        val COURSE_TIME = arrayOf(
            arrayOf(8, 30, 9, 10),
            arrayOf(9, 20, 10, 0),
            arrayOf(10, 20, 11, 0),
            arrayOf(11, 10, 11, 50),
            arrayOf(12, 0, 12, 40),
            arrayOf(13, 30, 14, 10),
            arrayOf(14, 20, 15, 0),
            arrayOf(15, 20, 16, 0),
            arrayOf(16, 10, 16, 50),
            arrayOf(17, 0, 17, 40),
            arrayOf(18, 30, 19, 10),
            arrayOf(19, 20, 20, 0),
            arrayOf(20, 10, 20, 50)
        )
    }
}