package tga.folder_sync.init


import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.japi.pf.ReceiveBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.exts.sec
import tga.folder_sync.files.FoldersFactory
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

    val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.init.InitActor")

    var srcTree: Tree<SFile>? = null
    var dstTree: Tree<SFile>? = null

    var resultListener: ActorRef? = null
    val taskTypes = mutableMapOf<String, String>()

    var srcScanned = 0
    var dstScanned = 0

    lateinit var printStatisticJob: Cancellable

    companion object {
        val pL1 = 1L.pL()
    }

    override fun createReceive() = ReceiveBuilder()
        .match(Perform::class.java               ) { initiateFoldersLoading() }
        .match(ScannerActor.Loaded::class.java   ) { handleLoadedResponse(it) }
        .match(ScannerActor.Statistic::class.java) { handleStatistic(it) }
        .match(PrintStat::class.java             ) { printStatistic("Scanning:") }

        .build()

    private fun printStatistic(title: String) {
        logger.info(title)
        logger.info("  src: $srcScanned files and folders")
        logger.info("  dst: $dstScanned files and folders")
    }

    private fun getTaskType(node: Tree<SFile>): String {
        val rootFolder = node.obj
        val taskKey = rootFolder.protocol + rootFolder.path
        val taskType = taskTypes[taskKey]!!
        return taskType
    }

    private fun handleStatistic(stat: ScannerActor.Statistic) {
        var node = stat.node
        while (node.parent != null) node = node.parent!!

        val taskType = getTaskType(node)
        when (taskType) {
            "src" -> srcScanned += stat.itemsScanned
            "dst" -> dstScanned += stat.itemsScanned
        }
    }

    private fun handleLoadedResponse(loadedResponse: ScannerActor.Loaded) {
        val taskType = getTaskType(loadedResponse.node)
        when (taskType) {
            "src" -> srcTree = loadedResponse.node
            "dst" -> dstTree = loadedResponse.node
        }
        tryCompareTrees()
    }

    private fun tryCompareTrees() {

        if (dstTree == null || srcTree == null) return

/*
        if (log().isDebugEnabled) {
            printTree("\nSource tree: ", srcTree!!)
            printTree("\nDestination tree: ", dstTree!!)
            println("")
        }
*/
        printStatisticJob.cancel()

        printStatistic("Files comparing...")

        val commands = srcTree!!.buildTreeSyncCommands(dstTree!!)

        val outDir: String = params.outDir + SimpleDateFormat("'.sync'-yyyy-MM-dd-HH-mm-ss").format(timestamp)

        logger.info("plan printing to: $outDir/plan.txt")
        val planHead = printCommands(outDir, commands, srcTree!!.obj, dstTree!!.obj)

        logger.info("\n$planHead")

        resultListener!!.tell(Done(outDir), self())
    }

    fun initiateFoldersLoading() {
        this.resultListener = sender()

        val source = params.src
        val destination = params.dst

        logger.info("New sync-session preparing started.")
        logger.info("    source: $source")
        logger.info("    destination: $destination")

        printStatisticJob = context.system.scheduler.scheduleAtFixedRate(
            1.sec(), 5.sec(), self(), PrintStat(), context.dispatcher, self()
        )

        requestScan(source, "src")
        requestScan(destination, "dst")

    }

    private fun requestScan(folderName: String, taskType: String) {
        val rootFolder: SFile = FoldersFactory.create(folderName)
        val taskKey = rootFolder.protocol + rootFolder.path
        taskTypes[taskKey] = taskType

        val rootFolderNode = Tree(rootFolder, null, mutableListOf())

        val props = ScannerActorFactory.props(rootFolderNode)
        val scannerActor = context.actorOf( props )

        val request = ScannerActor.Load(rootFolderNode, self(), self())
        scannerActor.tell( request, self() )
    }

    private fun printTree(title: String, tree: Tree<SFile>) {
        log().debug(title)
        tree.deepFirstTraversWithLevel(" ") { node, prefix->
            log().debug("${prefix}${node.obj.name}")
            "$prefix|   "
        }
    }

    private fun printCommands(outDir: String, commands: TreeSyncCommands<SFile>, srcFolder: SFile, dstFolder: SFile): String {
        val folder = File(outDir)
        if (!folder.exists()) folder.mkdir()

        var header = ""

            File("$outDir/plan.txt").printWriter().use { out ->

            val  sizeToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ it.obj.size } }
            val countToAdd    = commands.toAdd   .fold(0L){prev, el -> prev + el.sum{ 1           } }
            val  sizeToRemove = commands.toRemove.fold(0L){prev, el -> prev + el.sum{ 1           } }
            val countToRemove = commands.toRemove.count()

            val countTotal = countToAdd + countToRemove
            val  sizeTotal =  sizeToAdd + sizeToRemove

            header =
                "# A sync-session plan file\n" +
                "#  - session planned at: ${SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z").format(timestamp)}\n" +
                "#  -      source folder: ${srcFolder.protocol}${srcFolder.absolutePath}\n" +
                "#  - destination folder: ${dstFolder.protocol}${dstFolder.absolutePath}\n" +
                "#\n" +
                "#   total commands to run: ${countTotal.pL()}\n" +
                "#        total bytes sync: ${sizeTotal.pL()}"

            out.println(header)
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

        return header
    }

    data class Perform(val resultsListener: ActorRef)
    data class Done(val outDir:String)

    class PrintStat

}


