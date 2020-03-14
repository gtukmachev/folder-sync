package tga.folder_sync.tree

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
data class Tree<T : Comparable<T>>(
    val obj: T,
    val parent: Tree<T>?,
    val children: MutableList<Tree<T>> = mutableListOf()
){

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    
    fun deepFirstTravers(visit: (Tree<T>) -> Unit) {
        visit(this)
        for(node in children) node.deepFirstTravers(visit)
    }

    fun <D> deepFirstTraversWithLevel(d: D, visit: (Tree<T>, d: D) -> D) {
        val nextD = visit(this, d)
        for(node in children) node.deepFirstTraversWithLevel(nextD, visit)
    }

    /**
     * travers children only if the visit function return true
     */
    fun deepFirstTraversIf(visit: (Tree<T>) -> Boolean) {
        if (visit(this)) {
            for(node in children) node.deepFirstTraversIf(visit)
        }
    }

    fun <D> fold(visitRoot: (Tree<T>) -> D, visitChildren: (parentResult: D, child: Tree<T>) -> D) {

        val rootResult = visitRoot(this)

        this.foldChildren(rootResult, visitChildren)
    }

    private fun <D> foldChildren(parentResult: D, visitChildren: (parentResult: D, child: Tree<T>) -> D) {
        children.forEach {
            val childResult = visitChildren(parentResult, it)
            it.foldChildren(childResult, visitChildren)
        }
    }


    fun buildTreeSyncCommands(destinationTree: Tree<T>): TreeSyncCommands<T> {

        val addCommands = mutableListOf<Tree<T>>()
        val delCommands = mutableListOf<Tree<T>>()

        fun <X> Iterator<X>.nextOrNull() = if (hasNext()) next() else null

        fun scanNode(source: Tree<T>, destination: Tree<T>, prefix: String) {

            if (source.children.isEmpty() && destination.children.isEmpty()) return

            val srcIterator = source.children.iterator()
            val dstIterator = destination.children.iterator()

            var src: Tree<T>? = null
            var dst: Tree<T>? = null

            fun nextSrc() {src = srcIterator.nextOrNull(); logger.trace("${prefix}src = " + src?.obj)}
            fun nextDst() {dst = dstIterator.nextOrNull(); logger.trace("${prefix}dst = " + dst?.obj)}

            fun addToDst() {
                addCommands.add(src!!)
//                src!!.deepFirstTraversWithLevel(prefix){ srcNode, subPrefix ->
//                    addCommands.add(srcNode)
//                    logger.trace("$subPrefix add " + srcNode.obj)
//                    "$subPrefix    "
//                }
                nextSrc()
            }

            fun delFromDst() {
                delCommands.add(dst!!)
                logger.trace("$prefix del " + dst!!.obj)
                nextDst()
            }

            nextSrc(); nextDst()

            while (src != null || dst != null) {
                if      ( src == null && dst != null ) delFromDst()
                else if ( src != null && dst == null ) addToDst()
                else {
                    val cmp = src!!.obj.compareTo(dst!!.obj)
                    when {
                        (cmp >  0) -> delFromDst()
                        (cmp <  0) -> addToDst()
                        (cmp == 0) -> { scanNode(src!!, dst!!, prefix+"\t"); nextSrc(); nextDst() }
                    }
                }
            }

        }

        scanNode(this, destinationTree, "")

        return TreeSyncCommands(toAdd = addCommands, toRemove = delCommands)
    }


    override fun toString(): String {
        return "Tree{ children.size = ${children.size}, obj = $obj }"
    }
}