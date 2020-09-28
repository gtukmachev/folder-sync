package tga.folder_sync.it

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import org.junit.After
import org.junit.Before
import org.junit.Test
import tga.folder_sync.init.InitActor
import tga.folder_sync.params.Parameters
import tga.folder_sync.sync.SyncCoordinatorActor
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.*


abstract class AbstractItTest {

    abstract fun testMaxDuration(): Duration
    abstract fun prepareSource(): String
    abstract fun prepareDestination(): String
    abstract fun expectedInitPlan(date: String, sourceFolderName: String, destinationFolderName: String): String

    lateinit var system: ActorSystem

    @Before
    fun setup() {
        system = ActorSystem.create()
    }

    @After
    fun teardown() {
        TestKit.shutdownActorSystem(system)
    }

    @Test
    fun testInitAndSync() {
        object : TestKit(system){
            init {
                // prepare test data
                val sourceFolderName = prepareSource()
                val destinationFolderName = prepareDestination()

                val timestamp = Date()
                val params = Parameters(
                    command = Parameters.Command.`init`,
                    src = sourceFolderName,
                    dst = destinationFolderName,
                    sessionFolder = null,
                    outDir = "$sourceFolderName/../"
                )

                val initActor: ActorRef = system.actorOf(
                    Props.create(
                        InitActor::class.java,
                        timestamp,
                        params
                    ), "initActor"
                )

                // perform test action
                initActor.tell( InitActor.Perform(ref), ref )
                val initResult: InitActor.Done = expectMsgClass(InitActor.Done::class.java)
                val date = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(timestamp)

                syncPlanShouldBe(initResult.outDir) {
                    expectedInitPlan(date, sourceFolderName, destinationFolderName)
                }

                val syncActor = system.actorOf(Props.create(
                    SyncCoordinatorActor::class.java,
                    initResult.outDir
                ), "syncActor")

                syncActor.tell( SyncCoordinatorActor.Go(ref), ref )
                expectMsgClass( testMaxDuration(), SyncCoordinatorActor.Done::class.java )

                foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
            }
        }
    }

}
