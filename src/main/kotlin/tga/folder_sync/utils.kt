package tga.folder_sync

import java.text.NumberFormat

/**
 * Created by grigory@clearscale.net on 3/12/2019.
 */

private val sizeLength = 20

fun Number.pL() = NumberFormat.getIntegerInstance()
    .format(this).toString()
    .padStart(sizeLength)
