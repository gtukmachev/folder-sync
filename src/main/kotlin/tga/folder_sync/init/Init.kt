package tga.folder_sync.init


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.SFile
import tga.folder_sync.pL
import tga.folder_sync.tree.Tree
import tga.folder_sync.tree.TreeSyncCommands
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.init.init")
private val now = Date()


fun init(outDirPrefix: String, vararg args: String): String {
    if (args.size < 3) throw RuntimeException("not enough parameters!")

    val source = args[1]
    val destination = args[2]

    println("New sync-session preparing started.")
    println("    source: '$source'")
    println("    destination: '$destination'")


    println("\nThe source folder scanning...")
    val srcFolder = FoldersFactory.create(source)
    val srcTree = srcFolder.buildTree()

    println("\nThe destination folder scanning...")
    val dstFolder = FoldersFactory.create(destination)
    val dstTree = dstFolder.buildTree()

    if (logger.isDebugEnabled) {
        printTree("\nSource tree: ", srcTree)
        printTree("\nDestination tree: ", dstTree)
        println("")
    }

    println("\nFiles comparing...")
    val commands = srcTree.buildTreeSyncCommands(dstTree)


    println("\nplan printing...")
    val outDir: String = outDirPrefix + SimpleDateFormat("'.sync'-yyyy-MM-dd-HH-mm-ss").format(now)
    printCommands(outDir, commands, srcFolder, dstFolder)

    return outDir
}

private fun printTree(title: String, tree: Tree<SFile>) {
    println(title)
    tree.deepFirstTraversWithLevel(" ") { node, prefix->
        println("${prefix}${node.obj.name}")
        "$prefix|   "
    }

}

private val pL1 = 1L.pL()

private fun printCommands(outDir: String, commands: TreeSyncCommands<SFile>, srcFolder: SFile, dstFolder: SFile) {

    fun fileOrFolder(item: SFile) = when (item.isDirectory) {
        true  -> "<folder>"
        false -> "< file >"
    }

    val folder = File(outDir)

    if (!folder.exists()) {
        folder.mkdir()
    }


    File("$outDir/plan.txt").printWriter().use { out ->

        val  sizeToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ it.obj.size } }
        val countToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ 1           } }
        val  sizeToRemove = commands.toRemove.fold(0L){prev, el -> prev + el.sum{ 1           } }
        val countToRemove = commands.toRemove.count()

        val countTotal = countToAdd + countToRemove
        val  sizeTotal =  sizeToAdd + sizeToRemove

        out.println("# A sync-session plan file")
        out.println("#  - session planned at: ${SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(now)}")
        out.println("#  -      source folder: '${srcFolder.absolutePath}'")
        out.println("#  - destination folder: '${dstFolder.absolutePath}'")
        out.println("#")
        out.println("#   total commands to run: ${countTotal.pL()}")
        out.println("#        total bytes sync: ${sizeTotal.pL()}")
        out.println("#")

        for (srcTreeNode in commands.toAdd) {
            srcTreeNode.parentFirstTravers { treeNode ->
                val srcFile = treeNode.obj
                val dstFileName = "${dstFolder.protocol}${dstFolder.absolutePath}${dstFolder.pathSeparator}${srcFile.relativeTo(srcFolder) }".replace("\\", "/")

                if (srcFile.isDirectory) { ////// ----- create a folder -----
                    out.println("  mk <folder> |$pL1 | $dstFileName")

                } else { ////// ----- copy a file -----
                    val srcFileName = srcFile.absolutePath.replace("\\", "/")
                    out.println("copy < file > |${srcFile.size.pL()} | $srcFileName | $dstFileName")
                }


            }
        }

        for (dstNode in commands.toRemove) {
            val dstFile = dstNode.obj
            val dstFileName = "${dstFile.protocol}${dstFolder.absolutePath}${dstFolder.pathSeparator}${dstFile.relativeTo(dstFolder) }".replace("\\", "/")

            when(dstFile.isDirectory) {
                 true -> out.println(" del <folder> |${dstNode.volume().pL()} | $dstFileName")
                 else -> out.println(" del < file > |$pL1 | $dstFileName")
            }

        }
    }

}
