package com.cm.rxandroidble.util.extension

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException


suspend fun withTimeoutAndCallback(
    timeoutMillis: Long,
    operation: suspend () -> Unit,
    onTimeout: () -> Unit,
    onCompletion: () -> Unit,
    job: CompletableDeferred<Unit>
) {
    val timedOut = withTimeoutOrNull(timeoutMillis) {
        operation()
        try {
            job.await() // Wait for the job to be completed
            onCompletion()
        } catch (e: Exception) {
            if (e !is CancellationException) {
                // Handle other exceptions, if necessary
                Timber.e(":::withTimeoutAndCallback exception $e")
            }
        }
        false // Indicate successful completion or exception other than timeout
    } ?: true // Indicate timeout occurred

    if (timedOut) {
        onTimeout()
    }
}