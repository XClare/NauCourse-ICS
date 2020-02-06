package tool.naucourse.ics.contents.beans

import tool.naucourse.ics.Constants
import java.util.*

data class TermDate(
    // 周数为 0 表示假期
    val currentWeekNum: Int,
    val startDate: Date,
    val endDate: Date,
    val inVacation: Boolean = currentWeekNum == 0
) {

    init {
        if (currentWeekNum < 0 || currentWeekNum > Constants.Course.MAX_WEEKS_SIZE) {
            throw IllegalArgumentException("Term Info Week Number Error! Week Num: $currentWeekNum")
        }
        if (endDate.time <= startDate.time) {
            throw IllegalArgumentException("Term Info Term Start And End Date Error! Start Date: $startDate  End Date: $endDate")
        }
    }

    fun getTerm(): Term {
        val calendar = Calendar.getInstance(Locale.CHINA)
        calendar.time = startDate

        val year = calendar[Calendar.YEAR]

        return if (calendar[Calendar.MONTH] + 1 > 6) {
            Term(year, year + 1, 1)
        } else {
            Term(year - 1, year, 2)
        }
    }
}