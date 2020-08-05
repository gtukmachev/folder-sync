package tga.folder_sync.init


import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.files.SFile
import tga.folder_sync.pL
import tga.folder_sync.params.Parameters
import tga.folder_sync.tree.Tree
import tga.folder_sync.tree.TreeSyncCommands
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */

class InitActor(val timestamp: Date, val params: Parameters): AbstractLoggingActor() {

    var srcTree: Tree<SFile>? = null
    var dstTree: Tree<SFile>? = null

    var shutdownActor: ActorRef? = null

    companion object {
        val pL1 = 1L.pL()
    }

    override fun createReceive() = ReceiveBuilder()
        .match(Perform::class.java                       ) { initiateFoldersLoading() }
        .match(BuildTreeActor.SourceTree::class.java     ) { srcTree = it.tree; tryCompareTrees(); }
        .match(BuildTreeActor.DestinationTree::class.java) { dstTree = it.tree; tryCompareTrees(); }
        .build()

    private fun tryCompareTrees() {
        if (dstTree == null || srcTree == null) return

        if (log().isDebugEnabled) {
            printTree("\nSource tree: ", srcTree!!)
            printTree("\nDestination tree: ", dstTree!!)
            println("")
        }

        log().info("\nFiles comparing...")
        val commands = srcTree!!.buildTreeSyncCommands(dstTree!!)

        val outDir: String = params.outDir + SimpleDateFormat("'.sync'-yyyy-MM-dd-HH-mm-ss").format(timestamp)
        log().info("\nplan printing to: $outDir/plan.txt")
        printCommands(outDir, commands, srcTree!!.obj, dstTree!!.obj)

        shutdownActor!!.tell(Done(outDir), self())
    }

    fun initiateFoldersLoading() {
        this.shutdownActor = sender()

        val source = params.src
        val destination = params.dst

        val buildTreeActor = context.actorOf(Props.create( BuildTreeActor::class.java ), "buildActor")

        log().info("New sync-session preparing started.")
        log().info("    source: $source")
        log().info("    destination: $destination")

        buildTreeActor.tell( BuildTreeActor.Source(source),           self() )
        buildTreeActor.tell( BuildTreeActor.Destination(destination), self() )
    }

    private fun printTree(title: String, tree: Tree<SFile>) {
        log().debug(title)
        tree.deepFirstTraversWithLevel(" ") { node, prefix->
            log().debug("${prefix}${node.obj.name}")
            "$prefix|   "
        }
    }

    private fun printCommands(outDir: String, commands: TreeSyncCommands<SFile>, srcFolder: SFile, dstFolder: SFile) {
        val folder = File(outDir)
        if (!folder.exists()) folder.mkdir()

        File("$outDir/plan.txt").printWriter().use { out ->

            val  sizeToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ it.obj.size } }
            val countToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ 1           } }
            val  sizeToRemove = commands.toRemove.fold(0L){prev, el -> prev + el.sum{ 1           } }
            val countToRemove = commands.toRemove.count()

            val countTotal = countToAdd + countToRemove
            val  sizeTotal =  sizeToAdd + sizeToRemove

            out.println("# A sync-session plan file")
            out.println("#  - session planned at: ${SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(timestamp)}")
            out.println("#  -      source folder: ${srcFolder.protocol}${srcFolder.absolutePath}")
            out.println("#  - destination folder: ${dstFolder.protocol}${dstFolder.absolutePath}")
            out.println("#")
            out.println("#   total commands to run: ${countTotal.pL()}")
            out.println("#        total bytes sync: ${sizeTotal.pL()}")
            out.println("#")

            for (srcTreeNode in commands.toAdd) {
                srcTreeNode.parentFirstTravers { treeNode ->
                    val srcFile = treeNode.obj
                    val dstFileName = srcFile.relativeTo(srcFolder)

                    if (srcFile.isDirectory) {
                        out.println("   |   mk <folder> |$pL1 | $dstFileName")

                    } else {
                        val srcFileName = srcFile.relativeTo(srcFolder)
                        out.println("   | copy < file > |${srcFile.size.pL()} | $srcFileName")
                    }
                }
            }

            for (dstNode in commands.toRemove) {
                val dstFile = dstNode.obj
                val dstFileName = dstFile.relativeTo(dstFolder)

                when(dstFile.isDirectory) {
                    true -> out.println("   |  del <folder> |${dstNode.volume().pL()} | $dstFileName")
                    else -> out.println("   |  del < file > |$pL1 | $dstFileName")
                }
            }

        }

    }


    data class Perform(val resultsListener: ActorRef)
    data class Done(val outDir:String)
}


