package com.cm.rxandroidble.util.extension

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


enum class SleepType {
    START_SLEEP,
    STOP_SLEEP,
    DATA_SLEEP
}
internal fun LocalDateTime.YYMMDDHHMMSS(type: SleepType): String {
    val format = DateTimeFormatter.ofPattern("yyMMddHHmm", Locale.KOREAN).format(this)
    return when(type){
        SleepType.START_SLEEP -> {
            "W0" + format +"E"
        }
        SleepType.STOP_SLEEP ->{
            "W1" + format +"E"
        }
        SleepType.DATA_SLEEP ->{
            "W2" + format +"E"
        }
    }
}
