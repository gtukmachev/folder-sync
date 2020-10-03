package tga.utils

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Test
import kotlin.test.assertFalse

class SequenceTests {

    @Test
    fun nextTest() {

        val incomeLines = arrayOf("line-1", "line-2", "line-3")
        val seq = incomeLines.asSequence()
        val i = seq.iterator()

        assertThat( i.next(), `is`("line-1") )
        assertThat( i.next(), `is`("line-2") )
        assertThat( i.next(), `is`("line-3") )
        assertFalse( i.hasNext() )


    }

}
