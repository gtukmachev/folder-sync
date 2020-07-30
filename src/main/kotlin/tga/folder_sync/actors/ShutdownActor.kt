package tga.folder_sync.actors

import akka.actor.AbstractLoggingActor
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.init.InitActor

class ShutdownActor : AbstractLoggingActor() {

    override fun createReceive() = ReceiveBuilder()
    .match(InitActor.Done::class.java)  {  shutdown(it)  }
    .build()

    private fun shutdown(msg: Any) {
        log().info("Shutdown signal: $msg")
        context.system.terminate()
    }
}
