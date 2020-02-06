package tool.naucourse.ics.contents.base

data class ContentResult<T>(
    val isSuccess: Boolean,
    val contentErrorResult: ContentErrorReason = ContentErrorReason.NONE,
    val contentData: T? = null
)