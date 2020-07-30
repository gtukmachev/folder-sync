package tga.folder_sync.init

import akka.actor.AbstractLoggingActor
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.SFile
import tga.folder_sync.tree.Tree

class BuildTreeActor : AbstractLoggingActor() {

    override fun createReceive() = ReceiveBuilder()
            .match(     Source::class.java) { sender().tell(      SourceTree( buildTree(it.folder) ), self()) }
            .match(Destination::class.java) { sender().tell( DestinationTree( buildTree(it.folder) ), self()) }
        .build()

    private fun buildTree(folder: String): Tree<SFile> = FoldersFactory
        .create(folder)
        .buildTree()

    data class Source(val folder: String)
    data class SourceTree(val tree: Tree<SFile>)

    data class Destination(val folder: String)
    data class DestinationTree(val tree: Tree<SFile>)
}
