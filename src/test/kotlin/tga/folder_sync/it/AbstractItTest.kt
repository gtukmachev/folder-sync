package tga.folder_sync.it

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import org.junit.After
import org.junit.Before
import tga.folder_sync.exts.actorOf
import tga.folder_sync.init.InitActor
import tga.folder_sync.params.Parameters
import tga.folder_sync.sync.SyncInitiatorActor
import java.io.File
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

    open fun testInitAndSync() {
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

                system.actorOf("sync") {
                    SyncInitiatorActor(
                        planFile = File("${initResult.outDir}/plan.txt"),
                        numberOfFileCopyWorkers = 3,
                        requesterActor = ref
                    )
                }

                expectMsgClass( testMaxDuration(), SyncInitiatorActor.Done::class.java )

                foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
            }
        }
    }

}
