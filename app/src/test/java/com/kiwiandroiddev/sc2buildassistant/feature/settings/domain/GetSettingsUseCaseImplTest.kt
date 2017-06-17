package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.SettingsRepository
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl.GetSettingsUseCaseImpl
import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Created by Matt Clarke on 17/06/17.
 */
class GetSettingsUseCaseImplTest {

    @Mock lateinit var mockSettingsRepository: SettingsRepository

    lateinit var getSettingsUseCase: GetSettingsUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        getSettingsUseCase = GetSettingsUseCaseImpl(mockSettingsRepository)
    }

    @Test
    fun showAds_trueFromRepository_returnsTrue() {
        `when`(mockSettingsRepository.showAds()).thenReturn(Observable.just(true))

        val testObserver = getSettingsUseCase.showAds().subscribeTestObserver()

        testObserver.assertResult(true)
    }

    @Test
    fun showAds_falseFromRepository_returnsFalse() {
        `when`(mockSettingsRepository.showAds()).thenReturn(Observable.just(false))

        val testObserver = getSettingsUseCase.showAds().subscribeTestObserver()

        testObserver.assertResult(false)
    }

}
