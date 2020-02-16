package tga.folder_sync.files

import com.yandex.disk.rest.Credentials
import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
object FoldersFactory {

    fun create(name: String): SFile = when{
        (name.startsWith("yandex://")) -> YandexSFile(name.substring("yandex://".length), Credentials(System.getProperty("user"), System.getProperty("token")))
                                  else -> LocalSFile(File(name))
    }

}