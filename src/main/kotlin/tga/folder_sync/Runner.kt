package tga.folder_sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.init.Init
import tga.folder_sync.sync.Sync
import java.util.*

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync")

fun main(vararg args: String) {
    logger.debug("{}", args.joinToString(separator = " "))

    try {
        if (args.isEmpty()) throw RuntimeException("a command was not specified")

        when (args[0]) {
            "init" -> Init(System.getProperty("outDir") , Date(), *args).perform()
            "sync" -> Sync( if (args.size > 1) args[1] else null ).perform()
        }

    } catch (e: Exception) {
        println("Error: ${e.javaClass.simpleName} - ${e.message}")
        throw e
    }
}

