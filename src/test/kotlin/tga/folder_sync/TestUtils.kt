package tga.folder_sync

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import java.io.File
import kotlin.test.assertTrue

val rootFolder = "./target"

fun foldersShouldBeTheSame(sourceFolderName: String, destinationFolderName: String) {
    val srcTree = FolderUnit.fromFile(sourceFolderName)
    val dstTree = FolderUnit.fromFile(destinationFolderName)
    MatcherAssert.assertThat(srcTree, CoreMatchers.`is`(dstTree))
}


fun syncPlanShouldBe(outDirName: String, sourceDirName: String, planBuilder: () -> List<String> ) {
    val planFile = File(outDirName + "/plan.txt")
    assertTrue { planFile.exists() }

    val absoluteRootPrefix =  File(sourceDirName.substringBeforeLast("/")).absolutePath
    val expectation = planBuilder().map {
        it.replace("@-root-@", absoluteRootPrefix)
    }

    val planLines = planFile.readLines()

    MatcherAssert.assertThat(planLines.drop(5), CoreMatchers.`is`(expectation))
}
