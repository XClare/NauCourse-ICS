package tool.naucourse.ics.contents.beans

data class Course(
    val id: String,
    val name: String,
    val teacher: String,
    val courseClass: String?,
    val teachClass: String,
    val credit: Float,
    val type: String,
    val property: String?,
    var timeSet: Set<CourseTime>?
) {
    // For Current Term
    constructor(
        id: String,
        name: String,
        teacher: String,
        courseClass: String,
        teachClass: String,
        credit: Float,
        type: String,
        timeSet: Set<CourseTime>
    ) : this(id, name, teacher, courseClass, teachClass, credit, type, null, timeSet)

    // For Next Term
    constructor(
        id: String,
        name: String,
        teacher: String,
        teachClass: String,
        credit: Float,
        type: String,
        property: String,
        timeSet: Set<CourseTime>
    ) : this(id, name, teacher, null, teachClass, credit, type, property, timeSet)

    init {
        if (credit < 0) {
            throw IllegalArgumentException("Course Credit Error! Credit: $credit")
        }
        if (timeSet != null && timeSet!!.isEmpty()) {
            throw IllegalArgumentException("Course Time Is Empty!")
        }
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is Course) {
            other.id == id
        } else {
            false
        }
    }
}