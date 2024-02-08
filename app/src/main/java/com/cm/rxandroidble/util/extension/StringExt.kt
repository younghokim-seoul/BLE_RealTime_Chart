package com.cm.rxandroidble.util.extension

import timber.log.Timber
import kotlin.math.round

fun String.removeSleepCharacters(): String? {
    return if (this.length > 3 && this.startsWith("W") && this.endsWith("E")) {
        this.drop(2).dropLast(1)
    } else {
        null
    }
}

fun String.extractCharSleepType(): String {
    val stxIndex = this.indexOf('W')
    return this.substring(stxIndex, stxIndex + 2)
}

fun String.isMatchRawFormat(): Boolean = this.startsWith("S") && this.endsWith("E")
fun String.isMatchMeasureFormat(): Boolean = this.startsWith("R") && this.endsWith("E")

fun String.isMatchSleepFormat(): Boolean = this.startsWith("W") && this.endsWith("E")

fun String.isSleepDataFormat(): Boolean = this.startsWith("L") && this.endsWith("E")

fun String.isSleepTimeFormat(): Boolean = this.startsWith("T") && this.endsWith("E")




fun String.removeFirstChar(): String {
    return this.replaceFirst("^.".toRegex(), "")
}

fun String.removeLastChar(): String {
    return this.replaceFirst(".$".toRegex(), "")
}

fun ByteArray.byteArrayToHex(): String {
    val sb = StringBuilder(this.size * 2)
    for (b in this) sb.append(String.format("%02x", b))
    return sb.toString()
}

fun Float.calPercentage(totalCount: Int): Float {
    val percentage = (this / totalCount) * 100
    return round(percentage)
}

fun Int.formatMinutes(): String {
    return if (this < 60) {
        String.format("%01d분", this)
    } else {
        val hours = this / 60
        val remainingMinutes = this % 60
        String.format("%01d시간 %01d분", hours, remainingMinutes)
    }
}

fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

fun String.sleepTimeSplit(): List<String> {
    return this.chunked(2)
}
