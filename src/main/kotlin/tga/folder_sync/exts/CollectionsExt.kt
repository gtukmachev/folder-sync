package tga.folder_sync.exts

import java.util.*

fun <T> linkedListOf(vararg elements : T) = LinkedList<T>().apply { addAll(elements) }
