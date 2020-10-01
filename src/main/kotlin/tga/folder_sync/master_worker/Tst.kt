package tga.folder_sync.master_worker

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import com.typesafe.config.ConfigFactory
import tga.folder_sync.exts.sec
import tga.folder_sync.exts.sleep

fun main() {

    val config = ConfigFactory.parseString("""
        akka {
            loggers = ["akka.event.slf4j.Slf4jLogger"]
            logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"

            loglevel = "debug"
            // log-config-on-start = on
            log-dead-letters = 10
            log-dead-letters-during-shutdown = on
            
            actor {
                debug {
                    receive = on
                    autoreceive = on
                    lifecycle = on
                    unhandled = on
                }
            }
        }
    """.trimIndent())

    val akka = ActorSystem.create("tst", config)
    akka.actorOf(Props.create( TstMainActor::class.java ), "main")

}

class TstMainActor : AbstractLoggingActor() {

    lateinit var master: ActorRef

    override fun preStart() {
        super.preStart()
        master = context.actorOf( Props.create(TstMaster::class.java, self), "master")
    }

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(AllTasksDone::class.java){ log().debug("$it -> terminate the Actors system"); context.system.terminate() }
        .build()
}

data class TstTask(val i: Int)

class TstMaster(requester : ActorRef) : AbstractMasterActor<TstTask>(2, TstWorker::class, requester) {

    val tasks = Array(10, {it + 1})
        .map { TstTask(it) }
        .listIterator()

    override fun nextTask(): TstTask? = when {
        tasks.hasNext() -> { sleep(1.sec()); tasks.next(); }
        else            -> null
    }

    override fun onTaskDone(task: TstTask) {
        log().info("TstMaster.onTaskDone $task")
    }

    override fun onTaskErr(task: TstTask, err: Throwable) {
        log().info("TstMaster.onTaskErr $task, $err")
    }

}

class TstWorker(masterActor: ActorRef) : AbstractWorkerActor<TstTask>(masterActor) {
    override fun handleTask(task: TstTask) {
        log().info("---> Handling $task ...")
            when {
                (task.i % 3 == 0) -> throw RuntimeException("Wow!")
                else              -> sleep( 5.sec() )
            }
        log().info("<--- Handling $task")
    }
}

