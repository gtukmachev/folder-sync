package tga.folder_sync.exts

import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

val units = arrayOf("Bytes", "kB", "MB", "GB", "TB", "OktB")

fun Long.readableFileSize(): String {
    if (this <= 0) return "0"
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(this / 1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
}
