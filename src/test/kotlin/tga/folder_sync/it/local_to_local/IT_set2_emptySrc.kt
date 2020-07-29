package tga.folder_sync.it.local_to_local

import org.junit.Before
import org.junit.Test
import tga.folder_sync.init.Init
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe
import tga.folder_sync.sync.Sync
import java.text.SimpleDateFormat
import java.util.*

class IT_set2_emptySrc {
    @Before fun waitSec() { Thread.sleep(1000) }

    @Test fun initTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val init = Init("target\\", Date(),  "init", sourceFolderName, destinationFolderName)
        val outDirName = init.perform()

        val date = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(init.timestamp)

        syncPlanShouldBe(outDirName) {
            """
                    # A sync-session plan file
                    #  - session planned at: $date
                    #  -      source folder: $sourceFolderName
                    #  - destination folder: $destinationFolderName
                    #
                    #   total commands to run:                    6
                    #        total bytes sync:                   16
                    #
                      |  del < file > |                   1 | file0.txt
                      |  del <folder> |                   5 | sub-1
                      |  del <folder> |                   4 | sub-2
                      |  del <folder> |                   3 | sub-3
                      |  del <folder> |                   2 | sub-4
                      |  del <folder> |                   1 | sub-5
            """.trimIndent()
        }
    }

    @Test fun syncTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val init = Init("target\\", Date(),  "init", sourceFolderName, destinationFolderName)
        val outDirName = init.perform()

        val sync = Sync(outDirName)
        sync.perform()

        foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
    }

    private fun prepareSource() = localFolderStructure("tests-set2/src")
    private fun prepareDestination() =
        localFolderStructure("tests-set2/dst") {
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


}
