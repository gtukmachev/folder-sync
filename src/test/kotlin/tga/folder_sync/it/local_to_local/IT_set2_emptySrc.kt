package tga.folder_sync.it.local_to_local

import org.junit.Test
import tga.folder_sync.conf.Conf
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe

class IT_set2_emptySrc {

    @Test fun initTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\\\", "init", sourceFolderName, destinationFolderName)

        syncPlanShouldBe(outDirName, sourceFolderName) {
            listOf(
                "#   total commands to run:                    6",
                "#        total bytes sync:                   16",
                "#",
                " del < file > |                   1 | @-root-@/dst/file0.txt",
                " del <folder> |                   5 | @-root-@/dst/sub-1",
                " del <folder> |                   4 | @-root-@/dst/sub-2",
                " del <folder> |                   3 | @-root-@/dst/sub-3",
                " del <folder> |                   2 | @-root-@/dst/sub-4",
                " del <folder> |                   1 | @-root-@/dst/sub-5"
            )
        }
    }

    @Test fun syncTest() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        Conf.init()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\\\", "init", sourceFolderName, destinationFolderName)

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
