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
        val log: Logger = LoggerFactory.getLogger(YandexSFile::class.java)
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
        val thisPath = yandexFile.path!!.path
        if (thisPath.startsWith(base.path)) {
            return thisPath.substring(base.path.length + 1)
        }

        throw RuntimeException("incomparable")
    }

    override val protocol:      String get() = "yandex://"
    override val name:          String get() = yandexFile.name
    override val absolutePath:  String by lazy { "disk:"+ yandexFile.path!!.path }
    override val path:          String get() = yandexFile.path!!.path
    override val pathSeparator: String get() = "/"
    override val exists:       Boolean get() = true //todo CRITICAL implement or remove!
    override val isDirectory:  Boolean get() = yandexFile.isDir
    override val size:            Long get() = yandexFile.size

    override fun children(): List<SFile> {
        if (!yandexFile.isDir) return emptyList()

        var list: List<SFile>? = null
        try {
            val yandexResource = loadFromYandex(path)
            list = yandexResource.resourceList
                .items
                .asSequence()
                .map { YandexSFile(it) }
                .toList();
        } finally {
            if (log.isTraceEnabled) {
                log.trace("children '$path': ${list?.map{it.name}}")
            }
        }

        return list!!
    }

    override fun copyToIt(srcFile: LocalSFile) {
        log.trace("copy: ${srcFile.path} -> ${this.path}")
        val uploadLink = yandex.getUploadLink(this.yandexFile.path.path, true)
        yandex.uploadFile(
            uploadLink, true, srcFile.file, uploadProgressListener()
        )
    }

    override fun mkFolder() {
        log.trace("mkFolder: $absolutePath")
        val makeResp = yandex.makeFolder( path )
        // todo: implement response handling
    }

    override fun removeFile() {
        val logPrefix = "delete: $absolutePath"
        log.trace(logPrefix)

        val subTree = buildTree()

        subTree.deepFirstTravers{
            try {
                if (log.isTraceEnabled) log.trace("$logPrefix > ${it.obj.absolutePath}")
                yandex.delete(it.obj.path, false)
            } catch (e: HttpCodeException) {
                when {
                    e.code == 404 -> {} // resource not found
                    else -> throw RuntimeException(this.path, e)
                }
            }
        }
    }

    inner class uploadProgressListener : ProgressListener {
        override fun updateProgress(loaded: Long, total: Long) {
            val percent = if (total == 0L) 100 else loaded / total * 100
            log.trace("uploading `$path` :: $percent% ($loaded / $total) ")
        }

        override fun hasCancelled(): Boolean {
            return false
        }
    }
}

