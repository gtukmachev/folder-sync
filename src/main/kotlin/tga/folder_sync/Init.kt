package tga.folder_sync


import tga.folder_sync.files.FoldersFactory

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

    val changes = srcFolderTree.findChanges(dstFolderTree)


    println(changes)

}
