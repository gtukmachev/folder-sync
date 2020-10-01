package tga.folder_sync.sync

import akka.actor.ActorRef
import tga.folder_sync.master_worker.AbstractWorkerActor

class SyncWorkerActor(masterActor: ActorRef) : AbstractWorkerActor<SyncCmd>(masterActor) {

    override fun handleTask(task: SyncCmd) {
        task.perform( log() )
    }

}
