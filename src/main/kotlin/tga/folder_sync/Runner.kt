package tga.folder_sync

fun main(args: Array<String>) {

    try {
        if (args.isEmpty()) throw RuntimeException("a command was not specified")

        when (args[0]) {
            "init" -> init(args)
            "sync" -> sync(args)
        }

    } catch (e: Exception) {
        println("Error: ${e.javaClass.simpleName} - ${e.message}")
        throw e
    }
}

