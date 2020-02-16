package tga.folder_sync.yandex

import com.google.gson.Gson
import com.yandex.disk.rest.json.ResourceList
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YandexFilesSequenceIterator_Tests {

    private val gson = Gson()
    private val PAGE_SIZE = 10

    @Test fun tetsOneItemOnePage() {
        val yandexCl = mockk<com.yandex.disk.rest.RestClient>()

        val firstResp: ResourceList = gson.fromJson("""
            {
                "sort": true,
                "public_key": "",
                "path": "",
                "limit": ${PAGE_SIZE},
                "offset": ${0 * PAGE_SIZE},
                "total": 1,
                "items": [
                    {"name": "tetsOneItemOnePage.some"}
                ]
            }            
        """.trimIndent(), ResourceList::class.java)

        every { yandexCl.getFlatResourceList(match{ it.offset == 0 * PAGE_SIZE }) } returns firstResp
        every { yandexCl.getFlatResourceList(match{ it.offset == 1 * PAGE_SIZE }) } throws RuntimeException("Bad request")
        every { yandexCl.getFlatResourceList(match{ it.offset == 2 * PAGE_SIZE }) } throws RuntimeException("Bad request")

        val iterator = YandexFilesSequenceIterator(yandexCl, PAGE_SIZE)

        assertTrue("The first element should present") { iterator.hasNext() }
        assertTrue("The first element should be exact we prepared") { iterator.next().name == "tetsOneItemOnePage.some" }
        assertFalse("After getting the first element, iterator should has no more elements") { iterator.hasNext() }
    }

    @Test fun tetsOneElementTwoPages() {
        val yandexCl = mockk<com.yandex.disk.rest.RestClient>()

        val firstResp: ResourceList = gson.fromJson("""
            {
                "sort": true,
                "public_key": "",
                "path": "",
                "limit": ${PAGE_SIZE},
                "offset": ${0 * PAGE_SIZE},
                "total": 1,
                "items": [
                    {"name": "file.some"}
                ]
            }            
        """.trimIndent(), ResourceList::class.java)

        val secondResp: ResourceList = gson.fromJson("""
            {
                "sort": true,
                "public_key": "",
                "path": "",
                "limit": ${PAGE_SIZE},
                "offset": ${1 * PAGE_SIZE},
                "total": 0,
                "items": []
            }            
        """.trimIndent(), ResourceList::class.java)

        every { yandexCl.getFlatResourceList(match{ it.offset == 0 * PAGE_SIZE }) } returns firstResp
        every { yandexCl.getFlatResourceList(match{ it.offset == 1 * PAGE_SIZE }) } returns secondResp

        val iterator = YandexFilesSequenceIterator(yandexCl, PAGE_SIZE)

        assertTrue("The first element should present") { iterator.hasNext() }
        assertTrue("The first element should be exact we prepared") { iterator.next().name == "file.some" }
        assertFalse("After getting the first element, iterator should has no more elements") { iterator.hasNext() }
    }

}