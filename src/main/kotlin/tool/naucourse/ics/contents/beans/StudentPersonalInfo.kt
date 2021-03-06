package tool.naucourse.ics.contents.beans

data class StudentPersonalInfo(
    val stuId: Pair<String, String>,
    val name: Pair<String, String>,
    val grade: Pair<String, String>,
    val college: Pair<String, String>,
    val major: Pair<String, String>,
    val majorDirection: Pair<String, String>,
    val trainingDirection: Pair<String, String>,
    val currentClass: Pair<String, String>
)