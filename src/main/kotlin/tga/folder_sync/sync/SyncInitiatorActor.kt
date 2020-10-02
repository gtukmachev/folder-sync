package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import java.io.File

class SyncInitiatorActorr(
    val planFile: File,
    val numberOfWorkers: Int
) : AbstractLoggingActor() {

    lateinit var planLines: Array<String>
    lateinit var planUpdaterActor: ActorRef
    lateinit var statisticCollectorActor: ActorRef

    lateinit var srcRoot: String
    lateinit var dstRoot: String
             var totalFiles: Int = 0
             var totalSize: Long = 0L


    override fun preStart() {
        super.preStart()

        planUpdaterActor        = context.actorOf(Props.create())
        statisticCollectorActor = context.actorOf(Props.create())


        loadPlanFile()
        runMasterWorkerEnginePhase1()
    }

    private fun loadPlanFile() {
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
            val line = planLines[lineIndex]
            return try {
                line
                    .replace(",","")
                    .replace(".","")
                    .replace("'","")
                    .replace("`","")
                    .toLong()
            }  catch (e : Exception) {
                throw PlanFileFormatException(lineIndex + 1, "The number field in the line has wrong format (not recognized as an integer value): $line")
            }
        }

        srcRoot    = findStringParameter("#  -      source folder:")
        dstRoot    = findStringParameter("#  - destination folder:")
        totalFiles =   findLongParameter("#   total commands to run:").toInt()
        totalSize  =   findLongParameter("#        total bytes sync:")
    }

    private fun runMasterWorkerEnginePhase1() {
        val master = context.actorOf(Props.create(
            SyncMasterActor::class.java,
            1         , // numberOfWorkers: Int,
            self      , // requesterActor: ActorRef,
            planLines , // val planLines: Array<String>,
            srcRoot   , // val srcRoot: String,
            dstRoot   , // dstRoot: String,
            totalFiles, // totalFiles: Int,
            totalSize , // totalSize: Long,
            { s: String -> s.contains("mk <folder>") }, // incomeLinesFilter: inFilter?,
            planUpdaterActor        , // reportActor: ActorRef,
            statisticCollectorActor  // statisticCollectorActor: ActorRef
        ))
    }

    override fun createReceive() = createReceivePhase1()

    private fun createReceivePhase1(): Receive = ReceiveBuilder()
        .build()

    private fun createReceivePhase2(): Receive = ReceiveBuilder()
        .build()

}
