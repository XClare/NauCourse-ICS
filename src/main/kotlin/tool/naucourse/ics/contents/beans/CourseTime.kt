package tool.naucourse.ics.contents.beans

import tool.naucourse.ics.Constants

data class CourseTime(
    var courseId: String,
    val location: String,
    val weeksMode: WeeksMode,
    val weeks: Array<Time>,
    val weekDay: Short,
    val courseNum: Array<Time>
) {

    init {
        if (weekDay !in Constants.Time.MIN_WEEK_DAY..Constants.Time.MAX_WEEK_DAY) {
            throw IllegalArgumentException("Course Time Week Day Error! Week Day: $weekDay")
        }
    }

    enum class WeeksMode {
        SINGLE,
        DOUBLE,
        PLAIN
    }

    data class Time(
        val start: Int,
        val end: Int = start
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CourseTime

        if (courseId != other.courseId) return false
        if (location != other.location) return false
        if (weeksMode != other.weeksMode) return false
        if (!weeks.contentEquals(other.weeks)) return false
        if (weekDay != other.weekDay) return false
        if (!courseNum.contentEquals(other.courseNum)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = courseId.hashCode()
        result = 31 * result + location.hashCode()
        result = 31 * result + weeksMode.hashCode()
        result = 31 * result + weeks.contentHashCode()
        result = 31 * result + weekDay
        result = 31 * result + courseNum.contentHashCode()
        return result
    }
}