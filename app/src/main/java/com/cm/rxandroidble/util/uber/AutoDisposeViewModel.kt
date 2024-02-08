package com.cm.rxandroidble.util.uber


import androidx.lifecycle.ViewModel
import autodispose2.lifecycle.CorrespondingEventsFunction
import autodispose2.lifecycle.LifecycleEndedException
import autodispose2.lifecycle.LifecycleScopeProvider
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.BehaviorSubject

abstract class AutoDisposeViewModel : ViewModel(), LifecycleScopeProvider<AutoDisposeViewModel.ViewModelEvent> {


    private val lifecycleEvents = BehaviorSubject.createDefault(ViewModelEvent.CREATED)

    enum class ViewModelEvent {
        CREATED,
        CLEARED
    }

    override fun lifecycle(): Observable<ViewModelEvent> {
        return lifecycleEvents.hide()
    }

    override fun correspondingEvents(): CorrespondingEventsFunction<ViewModelEvent> {
        return CORRESPONDING_EVENTS
    }

    override fun peekLifecycle(): ViewModelEvent? {
        return lifecycleEvents.value
    }

    override fun onCleared() {
        lifecycleEvents.onNext(ViewModelEvent.CLEARED)
        super.onCleared()
    }

    companion object {
        private val CORRESPONDING_EVENTS =
            CorrespondingEventsFunction<ViewModelEvent> { event ->
                when (event) {
                    ViewModelEvent.CREATED -> ViewModelEvent.CLEARED
                    else ->
                        throw LifecycleEndedException("Cannot bind to ViewModel lifecycle after onCleared.")
                }
            }
    }
}