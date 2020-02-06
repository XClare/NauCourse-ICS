package tool.naucourse.ics.contents.base

data class RequestResult(
    val isRequestSuccess: Boolean,
    val requestContentErrorResult: ContentErrorReason = ContentErrorReason.NONE,
    val contentData: String? = null
)