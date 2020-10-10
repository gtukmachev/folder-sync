package tga.folder_sync.master_worker

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.on
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.KClass

abstract class AbstractMasterActor<T>(
    val numberOfWorkers: Int,
    val workerActorClass: KClass<out AbstractWorkerActor<out T>>,
    val requesterActor: ActorRef,
    private val workerName: String? = null
) : AbstractLoggingActor() {

    abstract fun nextTask(): T?
    abstract fun onTaskDone(task: T)
    abstract fun onTaskErr(task: T, err: Throwable)

    private var theJobIsDone = false
    private val activeWorkers = ArrayList<ActorRef>( numberOfWorkers )
    private val idleWorkers: Queue<ActorRef> = LinkedList<ActorRef>( )

    override fun preStart() {
        val workerActorName = workerName ?: workerActorClass.simpleName
        for (i in 1..numberOfWorkers)
            context.actorOf( Props.create(workerActorClass.java, self()), "$workerActorName-$i")
        log().debug("$numberOfWorkers workers ({}) were launched", workerActorClass.simpleName)
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .on(AbstractWorkerActor.Ready::class){
            if (log().isDebugEnabled) log().debug("The worker '{}' reported about his readiness", sender.path().name())
            idleWorkers += sender
            sendTaskToAWorker()
        }
        .on(AbstractWorkerActor.Response::class) {
            if (log().isDebugEnabled) log().debug("The worker '{}' reported about his task completing", sender.path().name())
            handleWorkerResponse(sender, it)
            sendTaskToAWorker()
        }
        .build()

    private fun handleWorkerResponse(worker: ActorRef, response: AbstractWorkerActor.Response<*>) {
        activeWorkers -= worker
        idleWorkers += worker
        when(response) {
            is AbstractWorkerActor.Done -> { log().debug("Task is   done: {}", response); onTaskDone(response.task as T)               }
            is AbstractWorkerActor.Err  -> { log().debug("Task is FAILED: {}", response); onTaskErr (response.task as T, response.err) }
        }
    }

    private fun sendTaskToAWorker() {
        while (idleWorkers.isNotEmpty()) {
            when (val task = nextTask()) {
                null -> { checkIfAllTasksAreDone(); sendTaskToAWorker@return }
                else -> {
                    val nextWorker = idleWorkers.remove()
                    log().debug("Sending task '{}' to worker `{}`", task, nextWorker.path().name())
                    activeWorkers += nextWorker
                    nextWorker.tell(AbstractWorkerActor.Run(task), self)
                }
            }
        }
    }

    private fun checkIfAllTasksAreDone() {
        if (!theJobIsDone) {
            if (activeWorkers.isEmpty()) {
                log().debug("All workers are finished their tasks -> reporting to requester '{}' about the completion", requesterActor.path())
                theJobIsDone = true
                requesterActor.tell(AllTasksDone("OK"), self)
            } else {
                if (log().isDebugEnabled) {
                    val workerNames = activeWorkers.map { it.path().name() }
                    log().debug("There are still some worker in progress: {}", workerNames)
                }
            }
        }
    }

}

data class AllTasksDone(val msg: String)
