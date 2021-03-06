package tga.folder_sync.files

import akka.event.LoggingAdapter
import com.google.gson.Gson
import com.squareup.okhttp.ConnectionPool
import com.squareup.okhttp.OkHttpClient
import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ProgressListener
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.exceptions.http.HttpCodeException
import com.yandex.disk.rest.json.Resource
import tga.folder_sync.exts.min
import tga.folder_sync.exts.readableFileSize
import tga.folder_sync.exts.sec
import java.util.concurrent.TimeUnit

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(val yandexFile: Resource) : SFile() {

    companion object {
//        val log: Logger = LoggerFactory.getLogger(YandexSFile::class.java)
        val credentials: Credentials = Credentials(System.getProperty("user"), System.getProperty("token"))
        val yandex: RestClient = RestClient(credentials, makeClient() )

        fun loadFromYandex(path: String): Resource {
            val req = ResourcesArgs.Builder()
                .setPath(path)
                .setLimit(10_000)
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

        fun makeClient(): OkHttpClient {
            val CONNECT_TIMEOUT_MILLIS = 30.sec().toMillis()
            val    READ_TIMEOUT_MILLIS = 15.min().toMillis()
            val   WRITE_TIMEOUT_MILLIS = 15.min().toMillis()

            val client = OkHttpClient()

            client.setConnectTimeout(CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
            client.setReadTimeout(READ_TIMEOUT_MILLIS,       TimeUnit.MILLISECONDS)
            client.setWriteTimeout(WRITE_TIMEOUT_MILLIS,     TimeUnit.MILLISECONDS)
            client.setConnectionPool(makeConnectionPool())
            client.dispatcher

            client.followSslRedirects = true
            client.followRedirects = true
            return client
        }

        fun makeConnectionPool(): ConnectionPool {
            val cp = ConnectionPool(30, 5 * 60 * 1000L)
            return cp
        }

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

        val yandexResource = loadFromYandex(path)
        val list: List<SFile> = yandexResource.resourceList
            .items
            .asSequence()
            .map { YandexSFile(it) }
            .toList();

        return list
    }

    override fun copyToIt(srcFile: LocalSFile, logger: LoggingAdapter) {
        val uploadLink = yandex.getUploadLink(this.yandexFile.path.path, true)
        yandex.uploadFile(
            uploadLink, true, srcFile.file, uploadProgressListener(logger)
            )
    }

    override fun mkFolder() {
        val makeResp = yandex.makeFolder( path )
        // todo: implement response handling
    }

    override fun removeFile() {
        val logPrefix = "delete: $absolutePath"

        val subTree = buildTree()

        subTree.deepFirstTravers{
            try {
                yandex.delete(it.obj.path, false)
            } catch (e: HttpCodeException) {
                when {
                    e.code == 404 -> {} // resource not found
                    else -> throw RuntimeException(this.path, e)
                }
            }
        }
    }

    inner class uploadProgressListener(private val logger: LoggingAdapter) : ProgressListener {
        var lastOutput = 0L
        var readableTotalFileSize: String? = null

            override fun updateProgress(loaded: Long, total: Long) {
                val nowMs = System.currentTimeMillis()
                when {
                    (loaded == total             ) -> logger.info("$path : 100% (${loaded.readableFileSize()})")
                    (nowMs - lastOutput < 30_000 ) -> {}
                    (loaded == 0L                ) -> {}
                    else -> {
                        if (readableTotalFileSize == null) readableTotalFileSize = total.readableFileSize()
                        val percent: Double = if (total == 0L) 100.0 else loaded.toDouble() / total.toDouble() * 100.0
                        logger.info("$path : ${percent.toInt()}% (${loaded.readableFileSize()} of $readableTotalFileSize) ")
                        lastOutput = nowMs
                    }
                }
        }

        override fun hasCancelled(): Boolean {
            return false
        }
    }
}

