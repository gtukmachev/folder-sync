package tga.folder_sync


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.SFile
import tga.folder_sync.tree.Tree
import tga.folder_sync.tree.TreeSyncCommands

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.init")


fun init(args: Array<String>) {
    if (args.size < 3) throw RuntimeException("not enough parameters!")

    val source = args[1]
    val destination = args[2]

    println("New sync-session preparing started.")
    println("    source: '$source'")
    println("    destination: '$destination'")


    val srcFolder = FoldersFactory.create(source)
    val dstFolder = FoldersFactory.create(destination)

    val srcTree = srcFolder.buildTree()
    val dstTree = dstFolder.buildTree()


    if (logger.isDebugEnabled) {
        printTree("\nSource tree: ", srcTree)
        printTree("\nDestination tree: ", dstTree)
        println("")
    }

    val commands = srcTree.buildSyncCommands(dstTree)


    if (logger.isDebugEnabled) {
        printCommands(commands, srcFolder, dstFolder)
    }

}

fun printTree(title: String, tree: Tree<SFile>) {
    println(title)
    tree.deepFirstTraversWithLevel(" ") { node, prefix->
        println("${prefix}${node.obj.name}")
        "$prefix|   "
    }

}

fun printCommands(commands: TreeSyncCommands<SFile>, srcFolder: SFile, dstFolder: SFile) {

    fun fileOrFolder(item: SFile) = when (item.isDirectory) {
        true  -> "<folder>"
        false -> "< file >"
    }



    for (src in commands.toAdd) {
        val srcFile = src.obj

        if (srcFile.isDirectory) {
            ////// ----- create a folder -----
            val dstFileName = "${dstFolder.absolutePath}${dstFolder.pathSeparator}${srcFile.relativeTo(srcFolder) }"

            println("  mk <folder> [$dstFileName]")

        } else {
            ////// ----- copy a file -----
            val srcFileName = srcFile.absolutePath
            val dstFileName = "${dstFolder.absolutePath}${dstFolder.pathSeparator}${srcFile.relativeTo(srcFolder) }"

            println("copy < file > [$srcFileName] [$dstFileName]")

        }
    }

    for (dstNode in commands.toRemove) {
        val dstFile = dstNode.obj
        val dstFileName = "${dstFolder.absolutePath}${dstFolder.pathSeparator}${dstFile.relativeTo(dstFolder) }"

        println(" del ${fileOrFolder(dstFile)} [$dstFileName]")
    }
}