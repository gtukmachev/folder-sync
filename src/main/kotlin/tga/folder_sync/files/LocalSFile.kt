package tga.folder_sync.files

import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class LocalSFile(val file: File) : SFile() {

    override val protocol:      String get() = ""
    override val name:          String by lazy { file.name        .replace("\\", "/") }
    override val absolutePath:  String by lazy { file.absolutePath.replace("\\", "/").replace("/./", "/") }
    override val path:          String by lazy { file.path        .replace("\\", "/").replace("/./", "/") }
    override val pathSeparator: String get() = System.getProperty("file.separator")
    override val exists:       Boolean get() = file.exists()
    override val isDirectory:  Boolean by lazy { file.isDirectory }
    override val size:            Long by lazy { if (isDirectory) 1 else file.length() }

    override fun children(): List<SFile> {
        return file.listFiles()!!
            .asSequence()
            .map { LocalSFile(it) }
            .toList()
    }

    override fun relativeTo(base: SFile): String {
        val baseFile = File(base.path)
        val rel = this.file.relativeTo( baseFile )
        return rel.path.replace("\\", "/")
    }

    override fun copyToIt(srcFile: LocalSFile) {
        srcFile.file.copyTo(this.file)
    }

    override fun mkFolder() {
        this.file.mkdirs()
    }

    override fun removeFile() {
        this.file.deleteRecursively()
    }
}
