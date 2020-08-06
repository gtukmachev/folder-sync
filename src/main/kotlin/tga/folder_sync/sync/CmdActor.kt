package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder

class CmdActor(val reportActor: ActorRef) : AbstractLoggingActor() {

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(SyncCmd::class.java) { handleCommand(it) }
        .build()

    private fun handleCommand(cmd: SyncCmd) {
        var result: SyncCmd = cmd
        var err: Throwable? = null

        try {
            result = cmd.perform()
        } catch(e: Throwable) {
            err = e
        }

        reportActor.tell( ReportActor.Done(result, err), self() )
    }

}
