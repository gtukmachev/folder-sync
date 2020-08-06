package tga.folder_sync

import java.time.Duration


fun Number.sec(): Duration = Duration.ofSeconds(this.toLong())
