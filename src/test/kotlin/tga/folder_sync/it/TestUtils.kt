package tga.folder_sync.it

import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.hamcrest.core.Is.`is`
import java.io.File
import kotlin.test.assertTrue

val localRootFolder = "target"
val yandexRootFolder = "tests"

fun foldersShouldBeTheSame(sourceFolderName: String, destinationFolderName: String) {
    val srcTree = FolderUnit.from(sourceFolderName)
    val dstTree = FolderUnit.from(destinationFolderName)
    MatcherAssert.assertThat(srcTree, CoreMatchers.`is`(dstTree))
}


fun syncPlanShouldBe(outDirName: String, planBuilder: () -> String) {
    val planFile = File("$outDirName/plan.txt")
    assertTrue { planFile.exists() }

    val expectation = planBuilder().split("\n")
    val planLines = planFile.readLines()

    MatcherAssert.assertThat(planLines, `is`(expectation))
}
