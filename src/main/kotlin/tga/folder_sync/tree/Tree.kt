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

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(this::class.java.declaringClass)
    }

    fun parentFirstTravers(visit: (Tree<T>) -> Unit) {
        visit(this)
        for(node in children) node.parentFirstTravers(visit)
    }

    fun deepFirstTravers(visit: (Tree<T>) -> Unit) {
        for(node in children) node.deepFirstTravers(visit)
        visit(this)
    }


    fun <D> deepFirstTraversWithLevel(d: D, visit: (Tree<T>, d: D) -> D) {
        val nextD = visit(this, d)
        for(node in children) node.deepFirstTraversWithLevel(nextD, visit)
    }

    fun <D> fold(visitRoot: (Tree<T>) -> D, visit: (parentResult: D, child: Tree<T>) -> D): D {
        val rootResult = visitRoot(this)
        return this.foldChildren(rootResult, visit)
    }

    fun sum(visit: (Tree<T>) -> Long) = fold( {root -> visit(root)} ){ prevResult, node -> prevResult + visit(node)}
    fun sumInt(visit: (Tree<T>) -> Int ) = fold( {root -> visit(root)} ){ prevResult, node -> prevResult + visit(node)}

    /**
     * Volume of the node subtree - Number of all sub-nodes on all levels, include this one.
     */
    fun volume(): Int = sumInt{ 1 }

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

            fun nextSrc() {src = srcIterator.nextOrNull()}
            fun nextDst() {dst = dstIterator.nextOrNull()}

            fun addToDst() {
                addCommands.add(src!!)
//                src!!.deepFirstTraversWithLevel(prefix){ srcNode, subPrefix ->
//                    addCommands.add(srcNode)
//                    "$subPrefix    "
//                }
                nextSrc()
            }

            fun delFromDst() {
                delCommands.add(dst!!)
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

    override fun toString() =  "Tree{obj = $obj, children.size = ${children.size}}"

    private fun <D> foldChildren(parentResult: D, visitChildren: (parentResult: D, child: Tree<T>) -> D): D {
        var result = parentResult
        children.forEach {
            result = visitChildren(result, it)
            result = it.foldChildren(result, visitChildren)
        }
        return result
    }

}
