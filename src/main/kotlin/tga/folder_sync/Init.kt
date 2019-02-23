package tga.folder_sync


import tga.folder_sync.files.Folder
import tga.folder_sync.files.FoldersFactory
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

    val srcFolderTree = srcFolder.buildTree()
    val dstFolderTree = dstFolder.buildTree()

    val commands = srcFolderTree.buildSyncCommands(dstFolderTree)


    printCommands(commands, dstFolder)

}


fun printCommands(commands: TreeSyncCommands<Folder>, dstFolder: Folder) {
    for (node in commands.toAdd) println("copy ${node.obj.absolutePath} ${dstFolder.absolutePath}${dstFolder.pathSeparator}${node.parent!!.obj.path}")

    for (node in commands.toRemove) println("del ${node.obj.absolutePath}")
}