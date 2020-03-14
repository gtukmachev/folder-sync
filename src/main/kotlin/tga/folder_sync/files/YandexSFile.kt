package tga.folder_sync.files

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(val yandexFile: Resource) : SFile() {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

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

    override fun relativeTo(base: SFile): String = TODO("not implemented")

    override val name: String get() = yandexFile.name

    override val absolutePath:  String get() = path
    override val path:          String get() = yandexFile.path!!.path
    override val pathSeparator: String get() = "/"
    override val exists:       Boolean get() = true
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

