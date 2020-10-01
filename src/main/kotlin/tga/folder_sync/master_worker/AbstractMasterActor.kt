package tga.folder_sync.master_worker

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import kotlin.reflect.KClass

abstract class AbstractMasterActor<T>(
    var numberOfWorkers: Int,
    var workerActorClass: KClass<out AbstractWorkerActor<out T>>,
    var requesterActor: ActorRef
) : AbstractLoggingActor() {

    abstract fun nextTask(): T?
    abstract fun onTaskDone(task: T)
    abstract fun onTaskErr(task: T, err: Throwable)

    private var theJobIsDone = false
    private val activeWorkers = HashSet<ActorRef>( numberOfWorkers )

    override fun preStart() {
        for (i in 1..numberOfWorkers)
            context.actorOf( Props.create(workerActorClass.java, self()), "${workerActorClass.simpleName}-$i")
        log().debug("$numberOfWorkers workers ({}) were launched", workerActorClass.simpleName)
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(AbstractWorkerActor.Ready::class.java   ) {
            if (log().isDebugEnabled) log().debug("The worker '{}' reported about his readiness", sender.path().name())
            sendTaskToWorker(sender)
        }
        .match(AbstractWorkerActor.Response::class.java) {
            if (log().isDebugEnabled) log().debug("The worker '{}' reported about his task completing", sender.path().name())
            handleWorkerResponse(sender, it)
            sendTaskToWorker(sender)
        }
        .build()

    private fun handleWorkerResponse(worker: ActorRef, response: AbstractWorkerActor.Response<*>) {
        activeWorkers -= worker
        when(response) {
            is AbstractWorkerActor.Done -> { log().debug("Task is   done: {}", response); onTaskDone(response.task as T)               }
            is AbstractWorkerActor.Err  -> { log().debug("Task is FAILED: {}", response); onTaskErr (response.task as T, response.err) }
        }
    }

    private fun sendTaskToWorker(worker: ActorRef) {
        when (val task = nextTask()) {
            null -> {
                log().debug("Stopping worker `{}` is stopped", worker.path().name())
                context.stop(worker)
                checkIfAllTasksAreDone()
            }
            else -> {
                log().debug("Sending task '{}' to worker `{}`", task, worker.path().name())
                activeWorkers += worker
                worker.tell(AbstractWorkerActor.Run(task), self)
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
