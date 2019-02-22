package tga.folder_sync.tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
data class Tree<T>(val obj: T, val children: MutableList<Tree<T>> = mutableListOf()){

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


    fun findChanges(destinationTree: Tree<out T>): TreeSyncCommands<T> {

        val toUpdate = mutableListOf<T>()
        val toDelete = mutableListOf<T>()

        fun checkSingleFolder(source: Tree<T>, destination: Tree<T>) {

            val srcIterator = source.children.iterator()
            val dstIterator = destination.children.iterator()

            while (dstIterator.hasNext() || srcIterator.hasNext()) {
                val srcNode = srcIterator.next()
                val dst = dstIterator.next()



            }

        }



        return TreeSyncCommands<T>(toAdd = toUpdate, toRemove = toDelete)

    }


}