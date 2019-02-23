package tga.folder_sync.files

import tga.folder_sync.tree.Tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
abstract class SFile : Comparable<SFile> {

    abstract val name: String
    abstract val absolutePath: String
    abstract val path: String
    abstract val pathSeparator: String
    abstract val exists: Boolean

    /**
     * <p>The function builds and returns full folders tree which contains all sub-folders.</p>
     *
     * @param ordered defines if the tree will be ordered or not. True by default.
     *                "ordered" means - all sub-folders inside a parent folder will be ordered by alphabet
     *
     */
    abstract fun buildTree(ordered: Boolean = true): Tree<SFile>

    override fun compareTo(other: SFile) = this.name.compareTo( other.name, ignoreCase = true )

    override fun toString() = path
}