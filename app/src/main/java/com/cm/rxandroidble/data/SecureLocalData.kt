package com.cm.rxandroidble.data

enum class PrefName(val value: String, val alias: String) {
    User("USER_PREFERENCES", "USER_PREFERENCES_SECURITY_ALIAS"),
}

sealed class SecureLocalData<T> {
    abstract val prefName: PrefName
    abstract val key: String
    abstract val default: T

    // primitive type이 아닌 경우 타입추론을 위해 사용합니다.
    open var complexType: Class<T>? = null

    object SleepId : SecureLocalData<Long>() {
        override val prefName: PrefName = PrefName.User
        override val key: String = "pref_sleep_id"
        override val default: Long = 0L
    }
}
