package com.cm.rxandroidble.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SleepDataModel(val sleepLevel: String, val isLast: Boolean) : Parcelable

