package tga.folder_sync.master_worker

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder

abstract class AbstractWorkerActor<T>(
    private val masterActor: ActorRef
) : AbstractLoggingActor() {

    class Ready
    data class Run<T>(val task: T)

    open class Response<T>
    data class Done<T>(val task: T): Response<T>()
    data class Err<T>(val task: T, val err: Throwable): Response<T>()

    override fun preStart() {
        masterActor.tell( Ready(), self )
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .match( Run::class.java ){ handleTaskWrapper(it) }
        .build()

    private fun handleTaskWrapper(runCommand: Run<*>) {
        try {
            val task: T = runCommand.task as T
            handleTask(task)
            masterActor.tell( Done(task), self )
        } catch (t: Throwable) {
            log().error(t, "Can't complete the task:\n{}\n. Reason:", runCommand)
            masterActor.tell( Err(runCommand.task, t), self )
        }
    }

    abstract fun handleTask(task: T)
}
