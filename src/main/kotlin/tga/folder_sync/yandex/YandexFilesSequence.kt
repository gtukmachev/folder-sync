package tga.folder_sync.yandex

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.ResourcesArgs
import com.yandex.disk.rest.RestClient
import com.yandex.disk.rest.json.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class YandexFilesSequence(credentials: Credentials) : Sequence<com.yandex.disk.rest.json.Resource> {
    val iterator = YandexFilesSequenceIterator(RestClient(credentials), 2000)
    override fun iterator() = iterator
}

class YandexFilesSequenceIterator(
    val yandexCl: RestClient,
    val pageSize: Int
) : Iterator<Resource> {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private var currentPage = -1

    private var noMoreDateAvailable = false
    private var isLastPage = false
    private var nextValue: Resource? = null
    private var currentIterator: Iterator<Resource>? = null;


    override fun hasNext(): Boolean {
        if (noMoreDateAvailable) return false
        if (nextValue != null) return true

        nextValue = tryToLoadNextValue()
        if (nextValue == null) {
            noMoreDateAvailable = true
            return false
        }

        return true
    }

    private fun tryToLoadNextValue(): Resource? {

//        logger.trace("tryToLoadNextValue() >>>")
        if (currentIterator != null && currentIterator!!.hasNext()) return currentIterator!!.next()//.also { logger.trace("tryToLoadNextValue() 1 <<< $it") }

        if (!isLastPage) {
            currentIterator = loadNextPageFromYandex()
        } else {
            //logger.trace("tryToLoadNextValue() 3 <<< null (lastPage)")
            return null
        }

        if (currentIterator != null && currentIterator!!.hasNext()) return currentIterator!!.next()//.also { logger.trace("tryToLoadNextValue() 2 <<< $it") }

        //logger.trace("tryToLoadNextValue() 3 <<< null (emptyNewPage)")
        return null
    }

    private fun loadNextPageFromYandex(): Iterator<Resource>? {
        currentPage += 1
        val req = ResourcesArgs.Builder()
            .setOffset(currentPage * pageSize)
            .setLimit(pageSize)
            .build()

        logger.trace("loadNextPageFromYandex() >>> page=$currentPage, req=$req")
        val resp = yandexCl.getFlatResourceList(req)
        isLastPage = resp.total < pageSize

        logger.trace("loadNextPageFromYandex(): <<< ${resp.items.size} items")
        return resp.items?.iterator()
    }

    override fun next(): Resource {
        if (nextValue != null) {
            val v = nextValue
            nextValue = null
            return v!!
        } else if (hasNext()) {
            val v = nextValue
            nextValue = null
            return v!!
        } else {
            throw RuntimeException("The YandexFilesSequenceIterator has no more elements")
        }
    }



}