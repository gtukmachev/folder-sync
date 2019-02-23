package tga.folder_sync.tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
data class TreeSyncCommands<T : Comparable<T>> (
        val toAdd: List<Tree<T>>,
        val toRemove: List<Tree<T>>
)