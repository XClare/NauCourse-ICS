package tool.naucourse.ics.contents.base

enum class ContentErrorReason {
    NONE,
    TIMEOUT,
    SERVER_ERROR,
    OPERATION,
    PARSE_FAILED,
    UNKNOWN
}