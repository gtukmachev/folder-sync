package tga.folder_sync.exts

fun Throwable.shortMsg(): String {
    val errClass = this::class.java.simpleName
    val errMsg = this.message
    return "$errClass: \"$errMsg\""
}
