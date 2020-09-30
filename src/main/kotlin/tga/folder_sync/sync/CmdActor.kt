package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder

class CmdActor(val reportActor: ActorRef) : AbstractLoggingActor() {

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(SyncCmd::class.java) { handleCommand(it) }
        .build()

    private fun handleCommand(cmd: SyncCmd) {
        log().debug(" -> {}", cmd)
        var result: SyncCmd = cmd
        var err: Throwable? = null

        try {
            // if ( cmd.lineNumber.rem( 2 ) == 0 ) throw RuntimeException("Test error")
            result = cmd.perform(log())
        } catch(e: Throwable) {
            err = e
        }

        log().debug(" <- {}", cmd)
        reportActor.tell( ReportActor.Done(result, err), self() )
    }

}
