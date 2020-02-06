package tool.naucourse.ics.contents.beans

data class Term(
    val startYear: Int,
    val endYear: Int,
    val termNum: Short
) {

    init {
        if (endYear < 0 || startYear < 0 || endYear - startYear != 1) {
            throw IllegalArgumentException("Term Year Error! Start: $startYear End: $endYear")
        }
        if (termNum !in 1..2) {
            throw IllegalArgumentException("Term Num Error! TermNum: $termNum")
        }
    }

    companion object {
        fun parse(text: String): Term {
            val mid = (text.length - 1) / 2
            val startYear = text.substring(0, mid).toInt()
            val endYear = text.substring(mid, text.length - 1).toInt()
            val termNum = text.substring(text.length - 1, text.length).toShort()
            return Term(startYear, endYear, termNum)
        }
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is Term) {
            other.toString() == toString()
        } else {
            false
        }
    }

    override fun toString(): String {
        return "$startYear$endYear$termNum"
    }
}