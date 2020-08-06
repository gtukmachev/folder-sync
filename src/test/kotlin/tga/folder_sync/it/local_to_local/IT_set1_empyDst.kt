package tga.folder_sync.it.local_to_local

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import tga.folder_sync.init.InitActor
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe
import tga.folder_sync.params.Parameters
import tga.folder_sync.sec
import tga.folder_sync.sync.SyncActor
import java.text.SimpleDateFormat
import java.util.*


class IT_set1_empyDst {

    companion object {
        lateinit var system: ActorSystem

        @BeforeClass
        @JvmStatic
        fun setup() {
            system = ActorSystem.create()
        }

        @AfterClass
        @JvmStatic
        fun teardown() {
            TestKit.shutdownActorSystem(system)
        }

    }

    @Test
    fun set1_init() {
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

                val result: InitActor.Done = expectMsgClass(InitActor.Done::class.java)
                val date = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(timestamp)

                syncPlanShouldBe(result.outDir) {
                    """
                    # A sync-session plan file
                    #  - session planned at: $date
                    #  -      source folder: $sourceFolderName
                    #  - destination folder: $destinationFolderName
                    #
                    #   total commands to run:                   16
                    #        total bytes sync:                  491
                    #
                       | copy < file > |                  56 | file0.txt
                       |   mk <folder> |                   1 | sub-1
                       | copy < file > |                  66 | sub-1/file-1.01.txt
                       | copy < file > |                  66 | sub-1/file-1.02.txt
                       |   mk <folder> |                   1 | sub-1/sub-1-1
                       | copy < file > |                  76 | sub-1/sub-1-1/file-1-1.01.txt
                       |   mk <folder> |                   1 | sub-2
                       |   mk <folder> |                   1 | sub-2/sub-2-1
                       | copy < file > |                  76 | sub-2/sub-2-1/file-2-1.01.txt
                       | copy < file > |                  76 | sub-2/sub-2-1/file-2-1.02.txt
                       |   mk <folder> |                   1 | sub-3
                       | copy < file > |                  66 | sub-3/file-3.01.txt
                       |   mk <folder> |                   1 | sub-3/sub-3-1
                       |   mk <folder> |                   1 | sub-4
                       |   mk <folder> |                   1 | sub-4/sub-4-1
                       |   mk <folder> |                   1 | sub-5
            """.trimIndent()
                }

            }
        }
    }

    @Test
    fun set1_sync() {
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

                val syncActor = system.actorOf(Props.create(
                    SyncActor::class.java,
                    initResult.outDir
                ), "syncActor")

                syncActor.tell( SyncActor.Perform(), ref )
                expectMsgClass( 10.sec(), SyncActor.Done::class.java )

                foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
            }

        }
    }

    private fun prepareSource() = localFolderStructure("tests-set1/src") {
        Txt("file0")
        Fld("sub-1") {
            Fld("sub-1-1") { Txt("file-1-1.01") }
            Txt("file-1.01")
            Txt("file-1.02")
        }
        Fld("sub-2") {
            Fld("sub-2-1") {
                Txt("file-2-1.01")
                Txt("file-2-1.02")
            }
        }
        Fld("sub-3") {
            Fld("sub-3-1")
            Txt("file-3.01")
        }
        Fld("sub-4") {
            Fld("sub-4-1") {}
        }
        Fld("sub-5")
    }

    private fun prepareDestination() = localFolderStructure("tests-set1/dst")

}
