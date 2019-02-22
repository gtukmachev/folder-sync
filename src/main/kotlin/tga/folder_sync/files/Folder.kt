package tga.folder_sync.files

import tga.folder_sync.tree.Tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
interface Folder {

    val name: String

    /**
     * <p>The function builds and returns full folders tree which contains all sub-folders.</p>
     *
     * @param ordered defines if the tree will be ordered or not. True by default.
     *                "ordered" means - all sub-folders inside a parent folder will be ordered by alphabet
     *
     */
    fun buildTree(ordered: Boolean = true): Tree<Folder>

}