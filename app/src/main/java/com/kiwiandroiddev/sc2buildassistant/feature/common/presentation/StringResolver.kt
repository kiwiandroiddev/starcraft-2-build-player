package com.kiwiandroiddev.sc2buildassistant.feature.common.presentation

interface StringResolver {
    fun getString(stringResourceId: Int): String
    fun getString(stringResourceId: Int, vararg formatArgs: Any): String
}