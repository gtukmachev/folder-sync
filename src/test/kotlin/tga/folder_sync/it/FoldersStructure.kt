package tga.folder_sync.it

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ProgressListener
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import com.yandex.disk.rest.json.Link
import com.yandex.disk.rest.json.Resource
import tga.folder_sync.files.YandexSFile
import java.io.File
import kotlin.random.Random

typealias folderContent = FolderUnit.() -> Unit
open class FolderUnit(val parent: FolderUnit?, val name: String) {

    companion object {

        val credentials: Credentials = Credentials(System.getProperty("user"), System.getProperty("token"))
        val yandex: RestClient = RestClient(credentials)

        fun loadFileInfoFromYandex(path: String): Resource {
            val req = ResourcesArgs.Builder()
                .setPath(path)
                .build()
            return yandex.getResources(req)
        }

        fun fromYandex(rootFolder: String): FolderUnit {

            fun addChildren(node: FolderUnit, nodePath: String) {
                val children = loadFileInfoFromYandex(nodePath).resourceList.items

                children.forEach {
                    val subNode: FolderUnit = when {
                        it.isDir -> FolderUnit(node, it.name)
                            else ->   FileUnit(node, it.name, "")
                    }
                    node.children.add(subNode)
                    if (it.isDir) addChildren(subNode, it.path.path)
                }
            }

            val root = FolderUnit(null, ".")
            addChildren(root, rootFolder)

            return root
        }

        fun fromFile(path: String): FolderUnit {
            val rootFile = File(localRootFolder)
            if (!rootFile.exists()) throw RuntimeException("The folder is not exists: '$localRootFolder'! ")

            val root = FolderUnit(null, ".")

            fun addChildren(node: FolderUnit, nodePath: String) {
                val children = File(nodePath).listFiles()!!.sorted()

                children.forEach {
                    val subNode: FolderUnit = when {
                        it.isDirectory -> FolderUnit(node, it.name)
                        else -> FileUnit(node, it.name, "")
                    }
                    node.children.add(subNode)
                    if (it.isDirectory) addChildren(subNode, it.path)
                }
            }

            addChildren(root, localRootFolder)

            return root
        }

    }

    val children = mutableListOf<FolderUnit>()

    fun clearLocal(){
        File(name).deleteRecursively()
    }

    fun clearYandex(){
        val delLink = try {
            yandex.delete(name, true)
        } catch(e: HttpCodeException) {
            when {
                e.code == 404 -> clearYandex@return // Resource not found
                         else -> throw e
            }
        }

        when (delLink.httpStatus) {
            Link.HttpStatus.inProgress -> {
                    var operation = yandex.getOperation(delLink)
                    var counter = 8
                    while(operation.isInProgress && counter > 0) {
                        Thread.sleep(1000)
                        operation = yandex.getOperation(delLink)
                        counter--
                    }
                    if (counter == 0 ) {
                        throw RuntimeException("Timeout exception during deletion of a folder from yandex disk: '$name'")
                    }
            }
            Link.HttpStatus.error -> {
                throw RuntimeException("An Error during deletion of a folder from yandex disk: \n$delLink")
            }
            else -> {}
        }

    }

    open fun makeLocal() {
        File(name).mkdirs()
        children.forEach { it.makeLocal() }
    }

    open fun makeYandex() {
        val folderFullName = name.substring("disk:/".length)
        val folders = folderFullName.split("/")
        var currentName = ""
        folders.forEach{
            currentName += "/$it"
            try { yandex.makeFolder(currentName) }
            catch (e : HttpCodeException) {
                when {
                    e.code == 409 && e.response.description.contains("existent directory") -> { }
                    else -> throw e
                }
            }

        }

        children.forEach { it.makeYandex() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FolderUnit) return false

        if (name != other.name) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    override fun toString(): String {
        return "FolderUnit(name='$name', children=$children)"
    }

    fun Fld(folderName: String, f: folderContent? = null) {
        val newChild = FolderUnit(this, this.name + "/" + folderName)
        this.children += newChild
        if (f != null) newChild.f()
    }

    fun Txt(fileName: String) {
        val newFile = FileUnit(this, this.name + "/" + fileName, "txt")
        this.children += newFile
    }

    fun localClearAndMake(): FolderUnit {
        clearLocal()
        makeLocal()
        return this
    }

    fun yandexClearAndMake(): FolderUnit {
        clearYandex()
        makeYandex()
        return this
    }

}

class FileUnit(parent: FolderUnit?, name: String, val type: String) : FolderUnit(parent, name) {
    override fun makeLocal() {
        val fullName = "$name.$type"
        File(fullName).appendText("$fullName\nAuto-generated test file")
    }

    override fun makeYandex() {
        fun randomFileName():String {
            val volume = 'z' - 'a' + 1
            fun rndChar(): Char = 'a'  + Random.nextInt(volume)

            val chars = charArrayOf(rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar(), rndChar())
            return String(chars)
        }

        File("./target/tmp_yandex_files").mkdirs()
        val fullName = "$name.$type"
        val srcFile = File("./target/tmp_yandex_files/${randomFileName()}.$type")
        srcFile.appendText("$fullName\nAuto-generated test file")

        val uploadLink = YandexSFile.yandex.getUploadLink(fullName, true)
        YandexSFile.yandex.uploadFile(
            uploadLink, true, srcFile, TestUploadProgressListener()
        )

        File(fullName).appendText("$fullName\nAuto-generated test file")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileUnit) return false
        if (!super.equals(other)) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileUnit(type='$type')"
    }

    inner class TestUploadProgressListener : ProgressListener {
        override fun updateProgress(loaded: Long, total: Long) {
        }

        override fun hasCancelled(): Boolean {
            return false
        }
    }

}

fun Fld(folderName: String, f: folderContent? = null): FolderUnit {
    val rootFolder = FolderUnit(null, folderName)
    if (f != null) rootFolder.f()
    return rootFolder
}

fun localFolderStructure(folderName: String, f: folderContent? = null) = Fld("$localRootFolder/$folderName", f)
    .localClearAndMake()
    .name

fun yandexFolderStructure(folderName: String, f: folderContent? = null) = Fld("$yandexRootFolder/$folderName", f)
    .yandexClearAndMake()
    .name
