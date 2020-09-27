package tga.folder_sync.exts

import java.time.Duration

fun Number.sec(): Duration = Duration.ofSeconds(this.toLong())
fun Number.min(): Duration = Duration.ofMinutes(this.toLong())
