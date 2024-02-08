package com.cm.rxandroidble.util.extension

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


enum class SleepType(val key: String) {
    START_SLEEP("W0"),
    STOP_SLEEP("W1"),
    DATA_SLEEP_REQ("W2"),
    SLEEP_DATA_START("W3"),
    SLEEP_DATA_STOP("W4");

    companion object {
        fun of(key: String): SleepType? = values().firstOrNull { it.key == key }
    }
}

internal fun LocalDateTime.YYMMDDHHMMSS(type: SleepType): String {
    val format = DateTimeFormatter.ofPattern("yyMMddHHmm", Locale.KOREAN).format(this)
    return when (type) {
        SleepType.START_SLEEP -> {
            "W0" + format + "E"
        }

        SleepType.STOP_SLEEP -> {
            "W1" + format + "E"
        }

        SleepType.DATA_SLEEP_REQ -> {
            "W2" + format + "E"
        }
        SleepType.SLEEP_DATA_START -> {
            "W3" + format + "E"
        }
        SleepType.SLEEP_DATA_STOP -> {
            "W4" + format + "E"
        }
    }
}

