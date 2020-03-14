package tga.folder_sync.files

import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class LocalSFile(val file: File) : SFile() {

    override val name:          String get() = file.name
    override val absolutePath:  String get() = file.absolutePath
    override val path:          String get() = file.path
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
        return rel.path
    }

}