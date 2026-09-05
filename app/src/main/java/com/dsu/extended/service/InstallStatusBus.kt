package com.dsu.extended.service

import com.dsu.extended.preparation.InstallationStep
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-global install event bus. The flashing code (owned by
 * DsuInstallService) emits; HomeViewModel collects while it is alive and
 * mirrors events onto its existing card-state handlers. Buffered so events
 * emitted before collection starts are not lost.
 */
object InstallStatusBus {

    sealed interface Event {
        data class Step(val step: InstallationStep) : Event
        data class Progress(val progress: Float, val partition: String) : Event
        data class Partition(val partition: String) : Event
        data class TerminalSuccess(val rooted: Boolean = true) : Event
        data class TerminalError(val step: InstallationStep, val text: String) : Event
        data object TerminalCanceled : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 64)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    fun emit(event: Event) {
        _events.tryEmit(event)
    }
}
