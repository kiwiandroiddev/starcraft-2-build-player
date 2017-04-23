package com.kiwiandroiddev.sc2buildassistant.feature.errorreporter

interface ErrorReporter {
    fun trackNonFatalError(error: Throwable)
}