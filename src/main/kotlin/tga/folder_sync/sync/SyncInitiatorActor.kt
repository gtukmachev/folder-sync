package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.actorOf
import tga.folder_sync.exts.readableFileSize
import tga.folder_sync.master_worker.AllTasksDone
import java.io.File

class SyncInitiatorActor(
    val planFile: File,
    val numberOfFileCopyWorkers: Int,
    val requesterActor: ActorRef
) : AbstractLoggingActor() {

    class Done

    lateinit var planLines: Array<String>
    lateinit var commandsSequence: Sequence<SyncCmd>
    lateinit var planUpdaterActor: ActorRef
    lateinit var statisticCollectorActor: ActorRef

    lateinit var srcRoot: String
    lateinit var dstRoot: String
             var totalFiles: Int = 0
             var totalSize: Long = 0L

    lateinit var masterActor_Folders: ActorRef
    lateinit var masterActor_Files: ActorRef
    lateinit var masterActor_Del: ActorRef

    override fun preStart() {
        super.preStart()

        loadPlanFile()

        planUpdaterActor = context.actorOf("plan"){
            PlanUpdaterActor( planFile.path, planLines)
        }

        statisticCollectorActor = context.actorOf("stat"){
            StatisticCollectorActor(totalFiles, totalSize)
        }

        runPhase1_Folders()
    }

    private fun loadPlanFile() {
        log().info("Loading synchronization plan file: {}", planFile.path)
        planLines = planFile.readLines().toTypedArray()

        fun findParameterLine(linePrefix: String): Int {
            val i = planLines.indexOfFirst { it.startsWith(linePrefix) }
            if (i == -1) throw PlanFileFormatException(-1, "Cant find required line in the plan file: $linePrefix..." )
            return i
        }

        fun findStringParameter(linePrefix: String): String {
            val line = planLines[findParameterLine(linePrefix)]
            return line.substring(linePrefix.length).trim()
        }

        fun findLongParameter(linePrefix: String): Long {
            val lineIndex = findParameterLine(linePrefix)
            val line = planLines[lineIndex].substring( linePrefix.length )
            return try {
                line.trim()
                    .replace(",","")
                    .replace(".","")
                    .replace("'","")
                    .replace("`","")
                    .toLong()
            }  catch (e : Exception) {
                throw PlanFileFormatException(
                    lineIndex + 1,
                    "The number field in the line has wrong format (not recognized as an integer value): ${planLines[lineIndex]}"
                )
            }
        }

        srcRoot    = findStringParameter("#  -      source folder:")
        dstRoot    = findStringParameter("#  - destination folder:")
        totalFiles =   findLongParameter("#   total commands to run:").toInt()
        totalSize  =   findLongParameter("#        total bytes sync:")

        log().info("""The sync plan is the following:
            |   srcRoot    = $srcRoot
            |   dstRoot    = $dstRoot
            |   totalFiles = ${totalFiles.readableFileSize()}
            |   totalSize  = ${totalSize.readableFileSize()}
        """.trimMargin())

        commandsSequence = planLines.asSequence()
            .mapIndexed{ i, line ->  SyncCmd.makeCommand(line, i+1, srcRoot, dstRoot) }
            .filter { it !is SkipCmd }
    }

    private fun runPhase1_Folders() {
        log().info("Starting of the first phase - Creating of all new folders on the target side.")
        context.become( phase1Behavior() )
        masterActor_Folders = context.actorOf("master-folders") {
            SyncMasterActor(
                numberOfWorkers = 1,
                requesterActor = self,
                syncCmdsSequence = commandsSequence.filter { it is MkDirCmd },
                planUpdaterActor = planUpdaterActor,
                statisticCollectorActor = statisticCollectorActor
            )
        }
    }

    private fun runPhase2_Files() {
        log().info("Starting of the third phase - deletion of files on the target side, which are gone from the original location")
        context.stop( masterActor_Folders )
        context.become( phase2Behavior() )
        masterActor_Files = context.actorOf("master-files") {
            SyncMasterActor(
                numberOfWorkers = numberOfFileCopyWorkers,
                requesterActor = self,
                syncCmdsSequence = commandsSequence.filter { (it !is MkDirCmd) && (it !is DelCmd) },
                planUpdaterActor = planUpdaterActor,
                statisticCollectorActor = statisticCollectorActor
            )
        }
    }

    private fun runPhase3_Files() {
        log().info("Starting of the second phase - copying of files to the destination.")
        context.stop( masterActor_Files )
        context.become( phase3Behavior() )
        masterActor_Del = context.actorOf("master-del") {
            SyncMasterActor(
                numberOfWorkers = numberOfFileCopyWorkers,
                requesterActor = self,
                syncCmdsSequence = commandsSequence.filter { it is DelCmd },
                planUpdaterActor = planUpdaterActor,
                statisticCollectorActor = statisticCollectorActor
            )
        }
    }

    private fun stopWorking(){
        log().info("All the sync phases are completed!")
        context.stop( masterActor_Del )
        context.stop( statisticCollectorActor )

        log().info("Updating of the plan file with the final status")
        context.become( ReceiveBuilder()
            .match(PlanUpdaterActor.ReportUpdaterIsDone::class.java){
                context.stop(planUpdaterActor)
                log().info("The plan file is finally updated.")
                requesterActor.tell( Done(), self )
            }
            .build() )
        planUpdaterActor.tell(PlanUpdaterActor.Finish(), self)


    }

    override fun createReceive(): Receive = phase1Behavior()

    private fun phase1Behavior(): Receive = ReceiveBuilder()
        .match(AllTasksDone::class.java) { runPhase2_Files() }
        .build()

    private fun phase2Behavior(): Receive = ReceiveBuilder()
        .match(AllTasksDone::class.java) { runPhase3_Files() }
        .build()

    private fun phase3Behavior(): Receive = ReceiveBuilder()
        .match(AllTasksDone::class.java) { stopWorking() }
        .build()

}
