package tga.folder_sync.files

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(val yandexFile: Resource) : SFile() {

    companion object {
        val credentials: Credentials = Credentials(System.getProperty("user"), System.getProperty("token"))
        val yandex: RestClient = RestClient(credentials)

        fun loadFromYandex(path: String): Resource {
            val req = ResourcesArgs.Builder()
                .setPath(path)
                .build()
            return yandex.getResources(req)
        }

        fun get(path: String) = YandexSFile(loadFromYandex(path))
    }

    override fun relativeTo(base: SFile): String {
        if (yandexFile.path?.path?.startsWith(base.path) == true) {
            return yandexFile.path.path.substring(base.path.length + 1)
        }

        throw RuntimeException("incomparable")
    }

    override val name: String get() = yandexFile.name

    override val absolutePath:  String get() = yandexFile.path!!.toString()
    override val path:          String get() = yandexFile.path!!.path
    override val pathSeparator: String get() = "/"
    override val exists:       Boolean get() = true //todo CRITICAL implement or remove!
    override val isDirectory:  Boolean get() = yandexFile.isDir
    override val size:            Long get() = yandexFile.size

    override fun children(): List<SFile> {
        val yandexResource = loadFromYandex(path)
        return yandexResource.resourceList
            .items
            .asSequence()
            .map { YandexSFile(it) }
            .toList();
    }
}

