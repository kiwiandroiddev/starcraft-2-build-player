package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import rx.Observable

class ResetDatabaseUseCaseImpl(val clearDatabaseUseCase: ClearDatabaseUseCase) : ResetDatabaseUseCase {

    override fun resetDatabase(): Observable<Void> = clearDatabaseUseCase.clear()

}