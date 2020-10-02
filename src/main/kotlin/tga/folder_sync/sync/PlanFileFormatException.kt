package tga.folder_sync.sync

class PlanFileFormatException(val lineNumber: Int, msg: String) : RuntimeException("[line = $lineNumber] $msg")
