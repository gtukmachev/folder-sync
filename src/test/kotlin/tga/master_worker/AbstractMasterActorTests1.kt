package tga.master_worker

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is
import org.junit.After
import org.junit.Before
import org.junit.Test
import tga.folder_sync.exts.actorOf
import tga.folder_sync.exts.linkedListOf
import tga.folder_sync.exts.sec
import tga.folder_sync.master_worker.AbstractMasterActor
import tga.folder_sync.master_worker.AbstractWorkerActor
import tga.folder_sync.master_worker.AllTasksDone
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class AbstractMasterActorTests1 {
    lateinit var system: ActorSystem

    @Before
    fun setup() {
        system = ActorSystem.create()
    }

    @After
    fun teardown() {
        TestKit.shutdownActorSystem(system)
    }

    fun allTasksDone(
        numberOfWorkers: Int,
        tasksList: List<String>,
        numberOfTaskGetAttempts: Int
    ) {
        object : TestKit(system){
            init {
                val tasks = LinkedList<String>().apply { addAll(tasksList) }
                val doneTasks = HashSet<String>()
                val nextTaskCounter = AtomicInteger(0)

                system.actorOf("master1"){ TestAbstractMasterActor1(
                    numberOfWorkers = numberOfWorkers,
                    requesterActor = ref,
                    tasksQueue = tasks,
                    doneTasks = doneTasks,
                    nextTaskCounter = nextTaskCounter
                ) }

                expectMsg(4.sec(), AllTasksDone("OK"))

                MatcherAssert.assertThat("All task should be consumed", tasks,     Is.`is`(linkedListOf()))
                MatcherAssert.assertThat("All task should be done", doneTasks, Is.`is`( HashSet<String>().apply{ addAll(tasksList) } ))
                MatcherAssert.assertThat("master.nextTask() should be invoked exact $numberOfTaskGetAttempts times", nextTaskCounter.get(), Is.`is`( numberOfTaskGetAttempts ))

            }
        }
    }

    @Test fun allTasksDone_1_2_3() { allTasksDone(1, linkedListOf("1", "2"), 3) }
    @Test fun allTasksDone_2_2_4() { allTasksDone(2, linkedListOf("1", "2"), 4) }
    @Test fun allTasksDone_3_2_5() { allTasksDone(3, linkedListOf("1", "2"), 5) }
    @Test fun allTasksDone_3_3_6() { allTasksDone(3, linkedListOf("1", "2", "3"), 6) }

    fun randomTest() {
            val nTasks = Random.nextInt(200)
            val nWorkers = Random.nextInt(10)
            val tasksList = Array(nTasks) { i -> "${i + 1}/$nTasks" }.asList()
            allTasksDone(
                numberOfWorkers = nWorkers,
                tasksList = tasksList,
                numberOfTaskGetAttempts = nTasks + nWorkers
            )
    }

    @Test fun random_first()  { randomTest() }
    @Test fun random_second() { randomTest() }
    @Test fun random_third()  { randomTest() }

}

class TestAbstractMasterActor1(
    numberOfWorkers: Int,
    requesterActor: ActorRef,
    val tasksQueue: Queue<String>,
    val doneTasks: MutableSet<String>,
    val nextTaskCounter: AtomicInteger
) : AbstractMasterActor<String>(
    numberOfWorkers  = numberOfWorkers,
    workerActorClass = TestsAbstractWorkerActor1::class,
    requesterActor   = requesterActor
) {

    override fun nextTask(): String? {
        nextTaskCounter.incrementAndGet()
        return tasksQueue.poll()
    }

    override fun onTaskDone(task: String) {
        log().info("onTaskDone('$task')")
        doneTasks += task
    }

    override fun onTaskErr(task: String, err: Throwable) {
        log().info("onTaskErr('$task', $err)")
    }
}

class TestsAbstractWorkerActor1(masterActor: ActorRef) : AbstractWorkerActor<String>(masterActor){
    override fun handleTask(task: String) {
        log().info(task)
    }
}
