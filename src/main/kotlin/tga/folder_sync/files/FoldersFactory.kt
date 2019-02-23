package tga.folder_sync.files

import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
object FoldersFactory {

    fun create(name: String): SFile {

        if (name.startsWith("yandex://")) return YandexSFile(name)

        return LocalSFile(File(name))

    }

}