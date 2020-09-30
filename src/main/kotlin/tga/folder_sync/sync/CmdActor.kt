package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder

class CmdActor(
    private val masterActor: ActorRef,
    private val reportActor: ActorRef,
    private val statisticActor: ActorRef
) : AbstractLoggingActor() {

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(SyncCmd::class.java) { handleCommand(it) }
        .build()

    private fun handleCommand(cmd: SyncCmd) {
        log().debug(" -> {}", cmd)
        var result: SyncCmd = cmd
        var err: Throwable? = null

        when (cmd) {
            is UnrecognizedCmd -> err = cmd.reason
            else -> {
                    try {
                        val wasDone = cmd.completed
                        result = cmd.perform(log())
                        if (!wasDone && result.completed) {
                            reportActor.tell( PlanUpdaterActor.UpdatePlanLine(result, err), self() )
                        }
                    } catch(e: Throwable) {
                        err = e
                        reportActor.tell( PlanUpdaterActor.UpdatePlanLine(result, err), self() )
                    }
            }
        }

        log().debug(" <- {}", cmd)
        statisticActor.tell( StatisticCollectorActor.UpdateStatisitc(result, err), self() )
        masterActor.tell(GiveMeNextTask(), self())
    }

    class GiveMeNextTask

}
