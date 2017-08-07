package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.SettingsRepository
import io.reactivex.Observable

class GetSettingsUseCaseImpl(val settingsRepository: SettingsRepository) : GetSettingsUseCase {

    override fun showAds(): Observable<Boolean> =
            settingsRepository.showAds()

}