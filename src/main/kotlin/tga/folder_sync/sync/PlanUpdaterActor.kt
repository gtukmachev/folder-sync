package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class PlanUpdaterActor(
    private val listenerActor: ActorRef,
    private val planFilePath: String
) : AbstractLoggingActor() {

    // External API:
    data class UpdatePlanLine(val cmd: SyncCmd, val err: Throwable?) // a request about updating of a line in the 'plan file'
    data class Finish(val id: Int = 0)    // a request for 'flushing' all the possible buffers to the 'plan file' and finishing
    // a response to this actor ownr about


    private val saveEvery = 1.min()         // update the plan file every 'this' time by a scheduled job
    private val saveNotOftenThan = 55.sec() // but not often than 'that' time period  (becoise, the saving can be forced between tasks)
    lateinit var periodicalPlanFileSaveJob: Cancellable // the 'periodical saving' job - sends the SaveFile() message to this actor every 'saveEvery' time period
    private var lastSaveTimestamp = 0L // when a last successful file saving was
    data class SaveFile(val id: Int = 0) // class for an internal message for initiating of a periodical file saving

    private val maxCapacity = 100 // how many lines we will save to a buffer before updating the file
    private var tasks: MutableList<UpdatePlanLine> = ArrayList(maxCapacity) // the lines buffer

    abstract class LastSaveResultsMessage
    data class LastSaveSuccess(val id: Int = 0) : LastSaveResultsMessage()
    data class LastSaveTimeout(val id: Int = 0) : LastSaveResultsMessage()

    lateinit var saver: ActorRef
    private var currentSaveRequestId = 0L

    lateinit var waitingForLastUpdateJob: Cancellable

    override fun preStart() {
        saver = context.actorOf("saver"){ PlanSaverActor(planFilePath) }
        periodicalPlanFileSaveJob = context.system.scheduler.scheduleAtFixedRate(
            saveEvery, saveEvery, self, SaveFile(), context.dispatcher, self
        )
    }

    override fun postStop() {
        periodicalPlanFileSaveJob.cancel()
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(                      UpdatePlanLine::class.java){ addNewTask(it) }
        .match(                            SaveFile::class.java){ saveFileNotTooOften() }
        .match(                              Finish::class.java){ initiateLastSaveChain() }
        .match( PlanSaverActor.TheFileIsWroteToDisk::class.java){ if (it.id >= currentSaveRequestId) lastSaveTimestamp = it.timestamp }
        .build()

    private fun waitingForLastUpdate(): Receive = ReceiveBuilder()
        .match( PlanSaverActor.TheFileIsWroteToDisk::class.java){ checkForLastUpdateIsDone(it) }
        .match(                     LastSaveTimeout::class.java){ forceStopWithTimeOut() }
        .build()


    private fun checkForLastUpdateIsDone(theFileIsWroteToDisk: PlanSaverActor.TheFileIsWroteToDisk) {
        if (currentSaveRequestId == theFileIsWroteToDisk.id) {
            waitingForLastUpdateJob.cancel()
            context.become( ReceiveBuilder().build() ) // do not react on messages anymore
            log().info("The plan file is finally updated: $planFilePath")
            listenerActor.tell( LastSaveSuccess() , self )
        }
    }

    private fun forceStopWithTimeOut() {
        context.become( ReceiveBuilder().build() ) // do not react on messages anymore
        listenerActor.tell( LastSaveTimeout(), self )
    }

    private fun addNewTask(task: UpdatePlanLine) {
        tasks.add(task); if (tasks.size >= maxCapacity) saveFile()
    }

    private fun initiateLastSaveChain() {
        saveFile()
        context.become( waitingForLastUpdate() )
        waitingForLastUpdateJob = context.system.scheduler.scheduleOnce(
            10.sec(), self, LastSaveTimeout(), context.dispatcher, self
        )
    }

    private fun saveFileNotTooOften() {
        if ((System.currentTimeMillis() - lastSaveTimestamp) > saveNotOftenThan.toMillis()) {
            saveFile()
        }
    }

    private fun saveFile() {
        val tasksToSend = tasks; tasks = ArrayList(maxCapacity)
        currentSaveRequestId ++
        saver.tell(PlanSaverActor.WriteFileToDisk( currentSaveRequestId, tasksToSend ), self)
    }

}

class PlanSaverActor(private val planFilePath: String) : AbstractLoggingActor() {

    data class WriteFileToDisk(val id: Long, val list: List<PlanUpdaterActor.UpdatePlanLine> )
    data class TheFileIsWroteToDisk(val id: Long, val timestamp: Long)

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(WriteFileToDisk::class.java){ writeFileToDisk(it); sender.tell(TheFileIsWroteToDisk(it.id, System.currentTimeMillis()), self) }
        .build()

    private fun writeFileToDisk(writeFileToDisk: WriteFileToDisk) {
        if (writeFileToDisk.list.isEmpty()) return

        val sortedTasks = writeFileToDisk.list.sortedBy { it.cmd.lineNumber }.iterator() // it should be SORTED externally!!!
        val reader = Files.newBufferedReader(Paths.get(planFilePath))
        val newFile = File("$planFilePath-${writeFileToDisk.id}")

        newFile.printWriter().use { writer ->
            var task: PlanUpdaterActor.UpdatePlanLine?  = sortedTasks.next()
            reader.useLines { oldLines -> oldLines.forEachIndexed { i, line ->
                var newLine = line
                if (task != null && task!!.cmd.lineNumber == (i + 1)) {
                    newLine = convertLine( line, task!! )
                    task = sortedTasks.nextOrNull()
                }
                writer.println( newLine )
            }}
        }

        newFile.copyTo(File(planFilePath), overwrite = true)
    }

    private fun convertLine(line: String, task: PlanUpdaterActor.UpdatePlanLine): String {
        val err = task.err ?: when (val cmd = task.cmd) {
            is UnrecognizedCmd -> cmd.reason
            else -> null
        }

        return when (err) {
            null -> " + |${line.substring(4)}"
            else -> "err|${line.substring(4)} | ${err.shortMsg()}"
        }
    }

}
