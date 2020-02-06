package tool.naucourse.ics.contents.beans

data class CourseSet(
    val courses: Set<Course>,
    val term: Term
) {
    init {
        if (courses.isEmpty()) {
            throw IllegalArgumentException("Course Set Is Empty!")
        }
    }
}