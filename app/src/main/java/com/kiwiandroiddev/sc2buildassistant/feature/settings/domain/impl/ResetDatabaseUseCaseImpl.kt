package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable

class ResetDatabaseUseCaseImpl(val clearDatabaseUseCase: ClearDatabaseUseCase) : ResetDatabaseUseCase {

    override fun resetDatabase(): Completable =
            clearDatabaseUseCase.clear()

}