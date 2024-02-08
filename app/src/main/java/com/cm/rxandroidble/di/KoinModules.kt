package com.cm.rxandroidble.di

import com.cm.rxandroidble.BleRepository
import com.cm.rxandroidble.data.DefaultSecureLocalDataStore
import com.cm.rxandroidble.data.SecureLocalDataStore
import com.cm.rxandroidble.viewmodel.BleViewModel
import com.cm.rxandroidble.viewmodel.SleepModeViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    single<SecureLocalDataStore> {
        DefaultSecureLocalDataStore(get())
    }
}

val viewModelModule = module {
    viewModel { BleViewModel(get(),get()) }
    viewModel { SleepModeViewModel(get(),get()) }
}

val repositoryModule = module{
    single{
        BleRepository()
    }
}