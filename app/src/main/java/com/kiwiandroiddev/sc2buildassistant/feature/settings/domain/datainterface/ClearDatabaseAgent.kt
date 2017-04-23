package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface

import io.reactivex.Completable

interface ClearDatabaseAgent {
    fun clear(): Completable
}