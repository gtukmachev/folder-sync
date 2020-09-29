package tga.folder_sync.init

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.files.SFile
import tga.folder_sync.tree.Tree

object ScannerActorFactory {

    fun props(subFolderNode: Tree<SFile>) = Props
        .create(ScannerActor::class.java)
        .withDispatcher("tree-load-dispatcher")

}

class ScannerActor : AbstractLoggingActor() {

    val tasks = mutableSetOf<ActorRef>()

    lateinit var request: Load

    override fun createReceive() = ReceiveBuilder()
            .match(Load::class.java) { request = it; scan() }
            .match(Loaded::class.java) { handleSubFolderResponse(it) }
        .build()

    private fun scan() {
        val root = request.node
        val rootFolder: SFile = request.node.obj
        log().debug("scan >>> $rootFolder")

        val subFolders = rootFolder.children()
        subFolders.sorted()

        subFolders.forEach { sFile ->
            val subNode = Tree(sFile, root)
            root.children.add(subNode)
            if (sFile.isDirectory) requestSubfolderScan( subNode )
        }

         tryRespond()
    }

    private fun requestSubfolderScan(subFolderNode: Tree<SFile>) {
        val props = ScannerActorFactory.props(subFolderNode)
        val scannerActor = context.actorOf( props )

        val task = Load(subFolderNode, self())
        tasks += scannerActor
        scannerActor.tell( task, self() )
    }

    private fun handleSubFolderResponse(loaded: Loaded) {
        tasks -= loaded.worker
        context.stop( loaded.worker )
        tryRespond()
    }

    private fun tryRespond() {
        if (tasks.isNotEmpty()) return

        val response = Loaded(request.node, self())

        log().debug("scan <<< ${request.node.obj}")
        request.requester.tell(response, self())
    }

    data class Load(val node: Tree<SFile>, val requester: ActorRef)
    data class Loaded(val node: Tree<SFile>, val worker: ActorRef)
}

