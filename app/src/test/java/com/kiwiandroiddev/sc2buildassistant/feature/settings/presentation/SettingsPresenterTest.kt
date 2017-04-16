package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import rx.Observable

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
internal class SettingsPresenterTest {

    @Mock lateinit var mockView: SettingsView
    @Mock lateinit var mockNavigator: SettingsNavigator
    @Mock lateinit var mockResetDatabaseUseCase: ResetDatabaseUseCase

    lateinit var presenter: SettingsPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = SettingsPresenter(mockResetDatabaseUseCase, mockNavigator)

        initDefaultMockBehaviors()
    }

    private fun initDefaultMockBehaviors() {
        `when`(mockResetDatabaseUseCase.resetDatabase()).thenReturn(Observable.just<Void>(null))
    }

    @Test
    fun attachView_doesntCrash() {
        presenter.attachView(mockView)
    }

    @Test
    fun showChangelogSelected_navigatesToFullChangelog() {
        presenter.showChangelogSelected()

        verify(mockNavigator).openFullChangelog()
    }

    @Test
    fun rateAppSelected_navigatesToPlayStoreListing() {
        presenter.rateAppSelected()

        verify(mockNavigator).openPlayStoreListing()
    }

    @Test
    fun translateSelected_opensTranslateUrl() {
        presenter.translateSelected()

        verify(mockNavigator).openUrl(SettingsPresenter.TRANSLATE_URL)
    }

    @Test
    fun resetDatabaseSelected_confirmResetDatabaseShownInView() {
        presenter.attachView(mockView)

        presenter.resetDatabaseSelected()

        verify(mockView).showResetDatabaseConfirmation()
    }

    @Test
    fun confirmResetDatabaseSelected_resetDatabaseUseCaseTriggered() {
        var resetTriggered = false
        `when`(mockResetDatabaseUseCase.resetDatabase()).thenReturn(
                Observable.just<Void>(null).doOnNext { resetTriggered = true }
        )
        presenter.attachView(mockView)

        presenter.confirmResetDatabaseSelected()

        assertThat(resetTriggered).isTrue()
    }

}

