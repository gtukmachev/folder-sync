package tga.folder_sync.it.local_to_yandex

import akka.actor.ActorRef
import akka.actor.Props
import akka.testkit.javadsl.TestKit
import org.junit.Test
import tga.folder_sync.exts.actorOf
import tga.folder_sync.exts.min
import tga.folder_sync.init.InitActor
import tga.folder_sync.it.*
import tga.folder_sync.params.Parameters
import tga.folder_sync.sync.SyncInitiatorActor
import java.io.File
import java.util.*
import kotlin.test.assertTrue


class IT_yandex_set1_empyDst : AbstractItTest() {

    override fun testMaxDuration() = 1.min()

    override fun prepareSource() = localFolderStructure("tests-set1/src") {
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

    override fun prepareDestination() = yandexFolderStructure("tests-set1/dst")

    override fun expectedInitPlan(date: String, sourceFolderName: String, destinationFolderName: String) = """
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

    @Test
    fun testPartialSync() {
        object : TestKit(system){

            init {
                val exclusions = listOf("sub-1", "sub-2", "sub-4")

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

                initActor.tell( InitActor.Perform(ref), ref )
                val initResult: InitActor.Done = expectMsgClass(InitActor.Done::class.java)

                val planFile = File("${initResult.outDir}/plan.txt")
                val planLines = planFile.readLines().toTypedArray()
                for (i in 0 until planLines.size) {
                    val l = planLines[i]
                    if (l[0] != '#') {
                        if (exclusions.any{ l.contains(it) } ) {
                            planLines[i] = " +" + l.substring(2)
                        }
                    }
                }
                planFile.printWriter().use { out ->
                    planLines.forEach( out::println )
                }

                system.actorOf("sync") {
                    SyncInitiatorActor(
                        planFile = File("$initResult.outDir/plan.txt"),
                        numberOfFileCopyWorkers = 3,
                        requesterActor = ref
                    )
                }

                expectMsgClass( testMaxDuration(), SyncInitiatorActor.Done::class.java )

                for (f in exclusions ) {
                    val folder = File("$localRootFolder/tests-set1/src/$f")
                    assertTrue { folder.exists() }
                    folder.deleteRecursively()
                }

                foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
            }

        }
    }


}
