package tga.folder_sync.files

import com.google.gson.Gson
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ProgressListener
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import com.yandex.disk.rest.json.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(val yandexFile: Resource) : SFile() {

    companion object {
        val logger: Logger = LoggerFactory.getLogger(YandexSFile::class.java)
        val credentials: Credentials = Credentials(System.getProperty("user"), System.getProperty("token"))
        val yandex: RestClient = RestClient(credentials)

        fun loadFromYandex(path: String): Resource {
            val req = ResourcesArgs.Builder()
                .setPath(path)
                .build()
            return try {
                    yandex.getResources(req)
                } catch (e: HttpCodeException) {


                when {
                        e.code == 404 -> {
                            Gson().fromJson("""
                                {
                                    "public_key" : "-1",
                                    "_embedded" : {
                                        "sort" : "false",
                                        "public_key" : "-2",
                                        "items" : [],
                                        "path" : "$path",
                                        "limit" : 0,
                                        "offset" : 0,
                                        "total" : 0
                                    },
                                    "name" : "$path",
                                    "created" : null,
                                    "public_url" : null,
                                    "origin_path" : null,
                                    "modified" : null,
                                    "deleted" : false,
                                    "path" : "$path",
                                    "md5" : null,
                                    "type" : null,
                                    "mime_type" : null,
                                    "preview" : null,
                                    "size" : 0,
                                    "custom_properties" : { }
                                }
                            """.trimIndent(), Resource::class.java )
                        }
                        else -> throw e
                    }
                }

        }

        fun get(path: String) = YandexSFile(loadFromYandex(path))
    }

    override fun relativeTo(base: SFile): String {
        if (yandexFile.path?.path?.startsWith(base.path) == true) {
            return yandexFile.path.path.substring(base.path.length + 1)
        }

        throw RuntimeException("incomparable")
    }

    override val protocol:      String get() = "yandex://"
    override val name:          String get() = yandexFile.name
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

    override fun copyToIt(srcFile: LocalSFile) {
        val uploadLink = yandex.getUploadLink(this.yandexFile.path.path, true)
        yandex.uploadFile(
            uploadLink, true, srcFile.file, uploadProgressListener()
        )
    }

    override fun mkFolder() {
        val makeResp = yandex.makeFolder( path )
        // todo: implement response handling
    }

    override fun removeFile() {
        try {
            yandex.delete(path, false)
        } catch (e: HttpCodeException) {
            when {
                e.code == 404 -> {} // resource not found
                         else -> throw RuntimeException(this.path, e)
            }
        }
    }

    inner class uploadProgressListener : ProgressListener {
        override fun updateProgress(loaded: Long, total: Long) {
            logger.info("$loaded / $total :: $path")
        }

        override fun hasCancelled(): Boolean {
            return false
        }
    }
}

