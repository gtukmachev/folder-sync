package tga.folder_sync.sync

import akka.actor.ActorRef
import tga.folder_sync.master_worker.AbstractMasterActor

class SyncMasterActor(
    numberOfWorkers: Int,
    requesterActor: ActorRef,
    val planLines: Array<String>,
    val srcRoot: String,
    val dstRoot: String,
    val totalFiles: Int,
    val totalSize: Long,
    val incomeLinesFilter: inFilter?,
    val reportActor: ActorRef,
    val statisticCollectorActor: ActorRef
) : AbstractMasterActor<SyncCmd>(numberOfWorkers, SyncWorkerActor::class, requesterActor) {

    var lineNumber = 0

    override fun nextTask(): SyncCmd? {
        if (lineNumber >= planLines.size) return null

        val line:String = planLines[lineNumber]; lineNumber++

        if (incomeLinesFilter != null && !incomeLinesFilter.invoke(line)) {
            return SkipCmd(lineNumber)
        }

        return SyncCmd.makeCommand(line, lineNumber, srcRoot, dstRoot)
    }

    override fun onTaskDone(task: SyncCmd) {
        reportActor.tell( PlanUpdaterActor.UpdatePlanLine(task, null), self )
        statisticCollectorActor.tell( StatisticCollectorActor.UpdateStatisitc(task, null), self )
    }

    override fun onTaskErr(task: SyncCmd, err: Throwable) {
        reportActor.tell( PlanUpdaterActor.UpdatePlanLine(task, err), self )
        statisticCollectorActor.tell( StatisticCollectorActor.UpdateStatisitc(task, err), self )
    }
}
