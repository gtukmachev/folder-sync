/*
package tga.folder_sync.it.local_to_yandex

import org.junit.Before
import org.junit.Test
import tga.folder_sync.init.Init
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe
import tga.folder_sync.it.yandexFolderStructure
import tga.folder_sync.sync.Sync
import java.text.SimpleDateFormat
import java.util.*

class IT_set1_emptyDst_yandex {
    @Before fun waitSec() { Thread.sleep(1000) }

    @Test fun set1_init() {
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

    @Test fun set1_sync() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        val init = Init("target\\", Date(),  "init", sourceFolderName, destinationFolderName)
        val outDirName = init.perform()

        // perform ta test action
        val sync = Sync(outDirName)
        sync.perform()

        foldersShouldBeTheSame(sourceFolderName, destinationFolderName)
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

    private fun prepareDestination() = yandexFolderStructure("tests-set1/dst")

}
*/
