package tga.folder_sync.it.local_to_local

import org.junit.Test
import tga.folder_sync.conf.Conf
import tga.folder_sync.it.foldersShouldBeTheSame
import tga.folder_sync.it.localFolderStructure
import tga.folder_sync.it.syncPlanShouldBe

class IT_set1_emptyDst {

    @Test fun set1_init() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\", "init", sourceFolderName, destinationFolderName)

        syncPlanShouldBe(outDirName, sourceFolderName) {
            listOf(
                "#   total commands to run:                   16",
                "#        total bytes sync:                  505",
                "#",
                "copy < file > |                  58 | @-root-@/src/file0.txt | @-root-@/dst/file0.txt",
                "  mk <folder> |                   1 | @-root-@/dst/sub-1",
                "copy < file > |                  68 | @-root-@/src/sub-1/file-1.01.txt | @-root-@/dst/sub-1/file-1.01.txt",
                "copy < file > |                  68 | @-root-@/src/sub-1/file-1.02.txt | @-root-@/dst/sub-1/file-1.02.txt",
                "  mk <folder> |                   1 | @-root-@/dst/sub-1/sub-1-1",
                "copy < file > |                  78 | @-root-@/src/sub-1/sub-1-1/file-1-1.01.txt | @-root-@/dst/sub-1/sub-1-1/file-1-1.01.txt",
                "  mk <folder> |                   1 | @-root-@/dst/sub-2",
                "  mk <folder> |                   1 | @-root-@/dst/sub-2/sub-2-1",
                "copy < file > |                  78 | @-root-@/src/sub-2/sub-2-1/file-2-1.01.txt | @-root-@/dst/sub-2/sub-2-1/file-2-1.01.txt",
                "copy < file > |                  78 | @-root-@/src/sub-2/sub-2-1/file-2-1.02.txt | @-root-@/dst/sub-2/sub-2-1/file-2-1.02.txt",
                "  mk <folder> |                   1 | @-root-@/dst/sub-3",
                "copy < file > |                  68 | @-root-@/src/sub-3/file-3.01.txt | @-root-@/dst/sub-3/file-3.01.txt",
                "  mk <folder> |                   1 | @-root-@/dst/sub-3/sub-3-1",
                "  mk <folder> |                   1 | @-root-@/dst/sub-4",
                "  mk <folder> |                   1 | @-root-@/dst/sub-4/sub-4-1",
                "  mk <folder> |                   1 | @-root-@/dst/sub-5"
            )
        }
    }

    @Test fun set1_sync() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        Conf.init()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\", "init", sourceFolderName, destinationFolderName)

        tga.folder_sync.sync.sync(outDirName)

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

    private fun prepareDestination() = localFolderStructure("tests-set1/dst")

}
