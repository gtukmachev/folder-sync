package tga.folder_sync.master_worker

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder

abstract class AbstractMasterActor<T>(
    var numberOfWorkers: Int,
    var workerActorClass: Class<in AbstractWorkerActor<T>>,
    var requesterActor: ActorRef
) : AbstractLoggingActor() {

    abstract fun nextTask(): T?
    abstract fun onTaskDone(task: T)
    abstract fun onTaskErr(task: T, err: Throwable)

    open class WorkerCommands
    class Done  : WorkerCommands()

    private var theJobIsDone = false
    private val activeWorkers = HashSet<ActorRef>( numberOfWorkers )

    override fun preStart() {
        for (i in 1..numberOfWorkers)
            context.actorOf( Props.create(workerActorClass, self()), "${workerActorClass.simpleName}-$i")
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(AbstractWorkerActor.Ready::class.java   ) {                                   sendTaskToWorker(sender) }
        .match(AbstractWorkerActor.Response::class.java) { handleWorkerResponse(sender, it); sendTaskToWorker(sender) }
        .build()

    private fun handleWorkerResponse(worker: ActorRef, response: AbstractWorkerActor.Response<*>) {
        activeWorkers -= worker
        when(response) {
            is AbstractWorkerActor.Done -> onTaskDone(response.task as T)
            is AbstractWorkerActor.Err  -> onTaskErr (response.task as T, response.err)
        }
    }

    private fun sendTaskToWorker(worker: ActorRef) {
        when (val task = nextTask()) {
            null -> { context.stop(worker); checkIfAllTasksAreDone() }
            else -> {
                activeWorkers += worker
                worker.tell(AbstractWorkerActor.Run(task), self)
            }
        }
    }

    private fun checkIfAllTasksAreDone() {
        if (!theJobIsDone) {
            if (activeWorkers.isEmpty()) {
                theJobIsDone = true
                requesterActor.tell(Done(), self)
            }
        }
    }

}
