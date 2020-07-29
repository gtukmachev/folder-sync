package tga.folder_sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.text.NumberFormat

/**
 * Created by grigory@clearscale.net on 3/12/2019.
 */

private val sizeLength = 20

fun Number.pL() = NumberFormat.getIntegerInstance()
    .format(this).toString()
    .padStart(sizeLength)

open class WithLogger {
    val log: Logger = LoggerFactory.getLogger( this::class.java.declaringClass )
}
