package tga.folder_sync

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

class IT_set1 {

    private val rootFolder = "./target"


    @Test fun set1_init() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\", "init", sourceFolderName, destinationFolderName)

        checkInitResults(outDirName, sourceFolderName)
    }

    @Test fun set1_sync() {
        // prepare test data
        val sourceFolderName = prepareSource()
        val destinationFolderName = prepareDestination()

        // perform ta test action
        val outDirName = tga.folder_sync.init.init("target\\", "init", sourceFolderName, destinationFolderName)

        tga.folder_sync.sync
            .sync(outDirName)
            .join()

        check_dst_is_the_dame_with_src(sourceFolderName, destinationFolderName)

    }

    private fun check_dst_is_the_dame_with_src(sourceFolderName: String, destinationFolderName: String) {
        val srcTree = FolderUnit.fromFile(sourceFolderName)
        val dstTree = FolderUnit.fromFile(destinationFolderName)

        assertThat( srcTree, `is`(dstTree) )

    }

    private fun checkInitResults(outDirName: String, sourceFolderName: String) {
        val planFile = File(outDirName + "/plan.txt")
        assertTrue { planFile.exists() }

        val absoluteRootPrefix =  File(sourceFolderName.substringBeforeLast("/")).absolutePath
        val expectation = listOf(
            "#   total files number to sync: [                   6] files",
            "#     total files size to sync: [                  63] bytes",
            "#",
            "copy < file > |                  58 | @-root-@\\src\\file0.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\file0.txt",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-1",
            "copy < file > |                  68 | @-root-@\\src\\sub-1\\file-1.01.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-1\\file-1.01.txt",
            "copy < file > |                  68 | @-root-@\\src\\sub-1\\file-1.02.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-1\\file-1.02.txt",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-1\\sub-1-1",
            "copy < file > |                  78 | @-root-@\\src\\sub-1\\sub-1-1\\file-1-1.01.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-1\\sub-1-1\\file-1-1.01.txt",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-2",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-2\\sub-2-1",
            "copy < file > |                  78 | @-root-@\\src\\sub-2\\sub-2-1\\file-2-1.01.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-2\\sub-2-1\\file-2-1.01.txt",
            "copy < file > |                  78 | @-root-@\\src\\sub-2\\sub-2-1\\file-2-1.02.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-2\\sub-2-1\\file-2-1.02.txt",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-3",
            "copy < file > |                  68 | @-root-@\\src\\sub-3\\file-3.01.txt | C:\\projects\\own\\folder_sync\\.\\target\\tests-set1\\dst\\sub-3\\file-3.01.txt",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-3\\sub-3-1",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-4",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-4\\sub-4-1",
            "  mk <folder> |                   1 | @-root-@\\dst\\sub-5"
        ).map{
            it.replace("@-root-@", absoluteRootPrefix)
        }

        val planLines = planFile.readLines()

        assertThat( planLines.drop(5), `is`(expectation) )

    }

    private fun prepareSource(): String {
        val files = Fld("$rootFolder/tests-set1/src"){
                Txt("file0")
                Fld("sub-1"){
                    Fld("sub-1-1"){ Txt("file-1-1.01") }
                    Txt("file-1.01")
                    Txt("file-1.02")
                }
                Fld("sub-2"){
                    Fld("sub-2-1"){
                        Txt("file-2-1.01")
                        Txt("file-2-1.02")
                    }
                }
                Fld("sub-3"){
                    Fld("sub-3-1")
                    Txt("file-3.01")
                }
                Fld("sub-4"){
                    Fld("sub-4-1"){}
                }
                Fld("sub-5")
        }
        files.clear()
        files.make()
        return files.name
    }

    private fun prepareDestination(): String {
        val files = Fld("$rootFolder/tests-set1/dst")
        files.clear()
        files.make()
        return files.name
    }



}
