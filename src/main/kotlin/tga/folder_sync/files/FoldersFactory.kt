package tga.folder_sync.files

import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
object FoldersFactory {

    fun create(name: String): SFile = when{
        (name.startsWith("yandex://")) -> YandexSFile.get(name.substring("yandex://".length))
                                  else -> LocalSFile(File(name))
    }

}