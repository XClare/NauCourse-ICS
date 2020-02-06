package tool.naucourse.ics.contents.base

data class ParseResult<T>(
    val isParseSuccess: Boolean,
    val parseData: T? = null
)