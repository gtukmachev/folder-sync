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