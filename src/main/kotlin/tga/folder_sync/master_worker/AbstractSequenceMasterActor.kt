package tga.folder_sync.master_worker

import akka.actor.ActorRef
import kotlin.reflect.KClass

abstract class AbstractSequenceMasterActor<T>(
    numberOfWorkers: Int,
    workerActorClass: KClass<out AbstractWorkerActor<out T>>,
    requesterActor: ActorRef,
    tasksSequence: Sequence<T>
) : AbstractMasterActor<T> (numberOfWorkers, workerActorClass, requesterActor, "worker"){

    private val tasksIterator = tasksSequence.iterator()

    override fun nextTask():T? = when(tasksIterator.hasNext()) {
        true -> tasksIterator.next().also { log().debug("new task from iterator: {}", it) }
        else -> {log().info("no more tasks in iterator"); null}
    }

}
