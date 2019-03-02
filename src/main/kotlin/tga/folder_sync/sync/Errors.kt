package tga.folder_sync.sync

class MasterActorCanBePerformedOnlyOnce : RuntimeException("You can't perform execution again. Create a new MasterActor for a new execution!")
data class UnrecognizedCommandFormat(val lineNumber: Int) : java.lang.RuntimeException()
