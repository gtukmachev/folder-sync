package tga.folder_sync.it.local_to_local

import org.junit.Test
import tga.folder_sync.conf.Conf
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe
import java.text.SimpleDateFormat

class IT_set2_emptySrc {

    @Test fun initTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\\\", "init", sourceFolderName, destinationFolderName)
        val date = SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(tga.folder_sync.init.now)

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
                     del < file > |                   1 | file0.txt
                     del <folder> |                   5 | sub-1
                     del <folder> |                   4 | sub-2
                     del <folder> |                   3 | sub-3
                     del <folder> |                   2 | sub-4
                     del <folder> |                   1 | sub-5
            """.trimIndent()
        }
    }

    @Test fun syncTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        Conf.init()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\", "init", sourceFolderName, destinationFolderName)

        tga.folder_sync.sync.sync(outDirName)

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
