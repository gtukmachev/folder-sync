package tga.folder_sync.tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
data class Tree<T : Comparable<T>>(
    val obj: T,
    val parent: Tree<T>?,
    val children: MutableList<Tree<T>> = mutableListOf()
){

    fun deepFirstTravers(visit: (Tree<T>) -> Unit) {
        visit(this)
        for(node in children) node.deepFirstTravers(visit)
    }

    /**
     * travers children only if the visit function return true
     */
    fun deepFirstTraversIf(visit: (Tree<T>) -> Boolean) {
        if (visit(this)) {
            for(node in children) node.deepFirstTraversIf(visit)
        }
    }


    fun buildSyncCommands(destinationTree: Tree<T>): TreeSyncCommands<T> {

        val addCommands = mutableListOf<Tree<T>>()
        val delCommands = mutableListOf<Tree<T>>()

        fun <X> Iterator<X>.nextOrNull() = if (hasNext()) next() else null

        fun checkSingleFolder(source: Tree<T>, destination: Tree<T>, prefix: String) {

            val srcIterator = source.children.iterator()
            val dstIterator = destination.children.iterator()

            var src: Tree<T>? = null
            var dst: Tree<T>? = null

            fun nextSrc() {src = srcIterator.nextOrNull(); println("${prefix}src = " + src?.obj)}
            fun nextDst() {dst = dstIterator.nextOrNull(); println("${prefix}dst = " + dst?.obj)}
            fun addToDst()   { addCommands.add(src!!); println("$prefix+++ " + src?.obj); nextSrc() }
            fun delFromDst() { delCommands.add(dst!!); println("$prefix--- " + dst?.obj); nextDst() }

            nextSrc(); nextDst()

            while (src != null || dst != null) {
                when {
                    ( src == null && dst != null ) -> delFromDst()
                    ( src != null && dst == null ) -> addToDst()

                    ( src!!.obj.compareTo(dst!!.obj) == 0 ) -> { checkSingleFolder(src!!, dst!!, prefix+"\t"); nextSrc(); nextDst(); }

                    ( src!!.obj > dst!!.obj) -> delFromDst()
                    ( src!!.obj < dst!!.obj) -> addToDst()
                }
            }

        }

        checkSingleFolder(this, destinationTree, "")

        return TreeSyncCommands(toAdd = addCommands, toRemove = delCommands)
    }


    override fun toString(): String {
        return "Tree{ children.size = ${children.size}, obj = $obj }"
    }
}