package tga.folder_sync

import akka.actor.*
import akka.japi.pf.DeciderBuilder
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.Resume
import tga.folder_sync.exts.sec


fun main() {
    val akka = ActorSystem.create("AkkaSystem")
    val testMainActor = akka.actorOf(Props.create(TestMainActor::class.java), "testMainActor")
    testMainActor.tell( "Run", ActorRef.noSender() )
}

class TestMainActor : AbstractLoggingActor() {

    lateinit var workerActor: ActorRef
    var waitCount: Int = 0

    var errCount: Int  = 0
    var successCount: Int = 0


    override fun preStart() {
        log().info("preStart")
        workerActor = context.actorOf(Props.create(WorkerActor::class.java), "workerActor")
    }

    override fun createReceive() = ReceiveBuilder()
        .match(String::class.java) { handleRun(it) }
        .match(Done::class.java) { handleDone(it) }
        .build()

    private val strategy = OneForOneStrategy( 10, 5.sec(),
        DeciderBuilder
                .match( Throwable::class.java, { sender == workerActor })
                    {
                        errCount++
                        checkIfJobIsDone()
                        Resume
                    }
        .build()
    )

    override fun supervisorStrategy() = strategy

    private fun handleDone(result: Done) {
        log().info("the '{}' handled with the result: {}", result.msg, result.n)
        successCount++
        checkIfJobIsDone()
    }

    private fun checkIfJobIsDone() {
        if ( (successCount + errCount) < waitCount) return

        log().info("The job done - all workers handled. succeed: {}, errs: {}", successCount, errCount)
        context.system.terminate()
    }

    private fun handleRun(msg: String) {
        log().info("{}", msg)

        workerActor.tell("- first  -", self())
        workerActor.tell("- second -", self())
        workerActor.tell("- third  -", self())

        waitCount = 3
    }

}

data class Done(val n: Int, val msg: Any?)

class WorkerActor : AbstractLoggingActor() {

    var counter: Int = 10

    override fun preStart() {
        counter = 0
        log().info("preStart")
    }

    override fun createReceive() = ReceiveBuilder()
        .matchAny{ handle(it) }
        .build()

    private fun handle(msg: Any?) {
        counter++
        val i = counter.rem(2)
        log().info("{} : {}", counter, msg)
        if (i == 1) throw RuntimeException("The count is odd")

        sender.tell(Done(counter, msg), self())
    }

}
