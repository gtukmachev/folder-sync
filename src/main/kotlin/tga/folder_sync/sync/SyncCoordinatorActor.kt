package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder

class SyncCoordinatorActor(
    val sessionFolderArg: String?,
    val copyThreads: Int
) : AbstractLoggingActor() {

    data class Go(val listerner: ActorRef)
    data class Done(
        val resultsPhase1: SyncActor.Done,
        val resultsPhase2: SyncActor.Done
    )

    var phase2started = false
    lateinit var listener: ActorRef

    lateinit var syncActorPhase1: ActorRef
    lateinit var syncActorPhase2: ActorRef

    lateinit var resultsPhase1: SyncActor.Done
    lateinit var resultsPhase2: SyncActor.Done

    override fun createReceive() = ReceiveBuilder()
        .match(Go::class.java                                                ) { m ->  listener = m.listerner; syncPhase1() }
        .match(SyncActor.Done::class.java, {_ -> sender() == syncActorPhase1}) { m -> resultsPhase1 = m;       syncPhase2() }
        .match(SyncActor.Done::class.java, {_ -> sender() == syncActorPhase2}) { m -> resultsPhase2 = m;       done()       }
        .build()

    /**
     * ### Phase 1:
     *
     * Make all required folders without parallelism
     * to avoid the situation when we create subfolder before it's parent was not committed yet.
     */
    private fun syncPhase1() {
        log().info("sync folders structure")
        syncActorPhase1 = context.actorOf( Props.create(
            SyncActor::class.java, // sessionFolderArg
            sessionFolderArg,      // nOfRoutes
            1,                     // incomeLinesFilter
            { s: String -> s.contains("mk <folder>") }
        ), "sync-folders")
        syncActorPhase1.tell( SyncActor.Perform(), self() )
    }

    /**
     * ### Phase 2:
     *
     * @see syncActorPhase1
     *
     * Copy and delete files in multi-treads mode
     */
    private fun syncPhase2() {
        if (phase2started) return
        phase2started = true

        context().stop(syncActorPhase1)
        log().info("sync files")
        syncActorPhase2 = context.actorOf( Props.create(
            SyncActor::class.java,
            sessionFolderArg, // sessionFolderArg
            copyThreads,      // nOfRoutes
            null              // incomeLinesFilter - no filter, because, on the phase 1, all commands "mk <folder>" waere done
        ), "sync-files")
        syncActorPhase2.tell( SyncActor.Perform(), self() )
    }

    private fun done() {
        context().stop(syncActorPhase2)
        listener.tell(
            Done(resultsPhase1, resultsPhase2),
            self
        )
    }


}
