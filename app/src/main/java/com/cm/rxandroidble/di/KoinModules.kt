package com.cm.rxandroidble.di

import com.cm.rxandroidble.BleRepository
import com.cm.rxandroidble.viewmodel.BleViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { BleViewModel(get()) }
}

val repositoryModule = module{
    single{
        BleRepository()
    }
}