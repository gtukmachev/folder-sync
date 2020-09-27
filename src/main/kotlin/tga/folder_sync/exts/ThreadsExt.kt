package tga.folder_sync.exts

import java.time.Duration

fun sleep(d: Duration) = Thread.sleep( d.toMillis() )
