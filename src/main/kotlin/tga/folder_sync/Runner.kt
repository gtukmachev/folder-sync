package tga.folder_sync

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.actors.ShutdownActor
import tga.folder_sync.init.InitActor
import tga.folder_sync.sync.SyncActor
import java.util.*

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync")
val akka = ActorSystem.create("AkkaSystem")

fun main(vararg args: String) {
    logger.debug("{}", args.joinToString(separator = " "))

    val shutdownActor = akka.actorOf(
        Props.create(
            ShutdownActor::class.java
        )
    )


    try {
        if (args.isEmpty()) throw RuntimeException("a command was not specified")

        when (args[0]) {
            "init" -> init(shutdownActor, *args)
            "sync" -> sync(shutdownActor, *args)
        }

    } catch (e: Exception) {
        println("Error: ${e.javaClass.simpleName} - ${e.message}")
        throw e
    }

    //akka.terminate()
}


fun init(shutdownActor: ActorRef, vararg args: String) {
    val initActor = akka.actorOf(
        Props.create(
            InitActor::class.java,
            System.getProperty("outDir"),
            Date(),
            args
        )
    )

    initActor.tell(InitActor.Perform(), shutdownActor)
}

fun sync(shutdownActor: ActorRef, vararg args: String) {
    val sessionFolder = if (args.size > 1) args[1] else null
    val syncActor = akka.actorOf(
        Props.create(
            SyncActor::class.java,
            sessionFolder
        )
    )

    syncActor.tell( SyncActor.Perform(), shutdownActor )
}
