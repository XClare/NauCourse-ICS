package tool.naucourse.ics.contents.beans

import java.util.*

data class Exam(
    val courseId: String,
    val name: String,
    val credit: Float,
    val teachClass: String,
    val startDate: Date,
    val endDate: Date,
    val location: String,
    val property: String,
    val type: String
)