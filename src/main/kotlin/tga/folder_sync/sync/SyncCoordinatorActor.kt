package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder

class SyncCoordinatorActor(
    val sessionFolderArg: String?
) : AbstractLoggingActor() {

    data class Go(val listerner: ActorRef)
    data class Done(
        val resultsPhase1: SyncActor.Done,
        val resultsPhase2: SyncActor.Done
    )

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

    private fun syncPhase1() {
        syncActorPhase1 = context.actorOf( Props.create(
            SyncActor::class.java, // sessionFolderArg
            sessionFolderArg,      // nOfRoutes
            1,                     // incomeLinesFilter
            { s: String -> s.contains("mk <folder>") }
        ))
        syncActorPhase1.tell( SyncActor.Perform(), self() )
    }

    private fun syncPhase2() {
        context().stop(syncActorPhase1)
        syncActorPhase2 = context.actorOf( Props.create(
            SyncActor::class.java,
            sessionFolderArg, // sessionFolderArg
            10,                // nOfRoutes
            null              // incomeLinesFilter - no filter, because, on the phase 1, all commands "mk <folder>" waere done
        ))
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
