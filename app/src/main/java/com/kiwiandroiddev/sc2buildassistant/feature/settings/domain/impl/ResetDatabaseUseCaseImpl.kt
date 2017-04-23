package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ClearDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable

class ResetDatabaseUseCaseImpl(val clearDatabaseUseCase: ClearDatabaseUseCase,
                               val loadStandardBuildsIntoDatabaseUseCase: LoadStandardBuildsIntoDatabaseUseCase)
    : ResetDatabaseUseCase {

    override fun resetDatabase(): Completable =
        clearDatabaseUseCase.clear().andThen(
            Completable.fromObservable(loadStandardBuildsIntoDatabaseUseCase.loadBuilds())
        )

}