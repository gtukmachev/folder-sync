package tga.folder_sync.sync

import akka.actor.ActorRef
import tga.folder_sync.master_worker.AbstractSequenceMasterActor

class SyncMasterActor(
    numberOfWorkers: Int,
    requesterActor: ActorRef,
    syncCmdsSequence: Sequence<SyncCmd>,
    val planUpdaterActor: ActorRef,
    val statisticCollectorActor: ActorRef
) : AbstractSequenceMasterActor<SyncCmd>(numberOfWorkers, SyncWorkerActor::class, requesterActor, syncCmdsSequence) {

    override fun onTaskDone(task: SyncCmd) {
        planUpdaterActor.tell( PlanUpdaterActor.UpdatePlanLine(task, null), self )
        statisticCollectorActor.tell( StatisticCollectorActor.UpdateStatisitc(task, null), self )
    }

    override fun onTaskErr(task: SyncCmd, err: Throwable) {
        planUpdaterActor.tell( PlanUpdaterActor.UpdatePlanLine(task, err), self )
        statisticCollectorActor.tell( StatisticCollectorActor.UpdateStatisitc(task, err), self )
    }
}
