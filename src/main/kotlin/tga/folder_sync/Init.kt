package tga.folder_sync


import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.SFile
import tga.folder_sync.tree.Tree
import tga.folder_sync.tree.TreeSyncCommands

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */

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


    printTree("\nSource tree: ", srcTree)
    printTree("\nDestination tree: ", dstTree)
    println("")

    val commands = srcTree.buildSyncCommands(dstTree)


    printCommands(commands, dstFolder)

}

fun printTree(title: String, tree: Tree<SFile>) {
    println(title)
    tree.deepFirstTraversWithLevel(" ") { node, prefix->
        println("${prefix}${node.obj.name}")
        "$prefix|   "
    }

}

fun printCommands(commands: TreeSyncCommands<SFile>, dstFolder: SFile) {
    for (src in commands.toAdd) {
        val folder =  src.parent!!.obj.path
        val firstSeparator = folder.indexOf(src.obj.pathSeparator)

        val dstPath = when (firstSeparator) {
              -1 -> ""
            else -> folder.substring(firstSeparator+1)
        }

        val dstAbsolutePath = "${dstFolder.absolutePath}${dstFolder.pathSeparator}$dstPath"

        println("copy folder [${src.obj.absolutePath}] to folder [$dstAbsolutePath]")
    }

    for (node in commands.toRemove) println("del folder [${node.obj.absolutePath}] out from the destination side")
}