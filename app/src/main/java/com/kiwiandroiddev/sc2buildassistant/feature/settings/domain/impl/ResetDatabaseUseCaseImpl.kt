package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.ClearDatabaseAgent
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable

class ResetDatabaseUseCaseImpl(val clearDatabaseAgent: ClearDatabaseAgent,
                               val loadStandardBuildsIntoDatabaseUseCase: LoadStandardBuildsIntoDatabaseUseCase)
    : ResetDatabaseUseCase {

    override fun resetDatabase(): Completable =
        clearDatabaseAgent.clear().andThen(
            Completable.fromObservable(loadStandardBuildsIntoDatabaseUseCase.loadBuilds())
        )

}