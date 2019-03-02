package tga.folder_sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.sync.sync

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync")

fun main(args: Array<String>) {

    logger.info("{}", args.joinToString(separator = " "))

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

