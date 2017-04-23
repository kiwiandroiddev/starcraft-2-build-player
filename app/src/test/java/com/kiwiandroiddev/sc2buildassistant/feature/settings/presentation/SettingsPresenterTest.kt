package com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

internal class SettingsPresenterTest {

    @Mock lateinit var mockView: SettingsView
    @Mock lateinit var mockNavigator: SettingsNavigator
    @Mock lateinit var mockErrorReporter: ErrorReporter
    @Mock lateinit var mockResetDatabaseUseCase: ResetDatabaseUseCase

    lateinit var presenter: SettingsPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = SettingsPresenter(
                resetDatabaseUseCase = mockResetDatabaseUseCase,
                navigator = mockNavigator,
                errorReporter = mockErrorReporter,
                executionScheduler = Schedulers.trampoline(),
                viewResultScheduler = Schedulers.trampoline())

        initDefaultMockBehaviors()
    }

    private fun initDefaultMockBehaviors() {
        `when`(mockResetDatabaseUseCase.resetDatabase()).thenReturn(Completable.complete())
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

        verify(mockNavigator).openTranslationPage()
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
                Completable.complete().doOnComplete { resetTriggered = true }
        )
        presenter.attachView(mockView)

        presenter.confirmResetDatabaseSelected()

        assertThat(resetTriggered).isTrue()
    }

    @Test
    fun confirmResetDatabase_errorOccurs_showsErrorInView() {
        val errorMessage = "IO error"
        `when`(mockResetDatabaseUseCase.resetDatabase())
                .thenReturn(Completable.error(RuntimeException(errorMessage)))
        presenter.attachView(mockView)

        presenter.confirmResetDatabaseSelected()

        verify(mockView).showResetDatabaseError(errorMessage)
    }

    @Test
    fun confirmResetDatabase_errorOccurs_nonFatalErrorReported() {
        val error = RuntimeException("IO error")
        `when`(mockResetDatabaseUseCase.resetDatabase())
                .thenReturn(Completable.error(error))
        presenter.attachView(mockView)

        presenter.confirmResetDatabaseSelected()

        verify(mockErrorReporter).trackNonFatalError(error)
    }

    @Test
    fun confirmResetDatabaseSelected_noErrors_showsSuccessInView() {
        `when`(mockResetDatabaseUseCase.resetDatabase()).thenReturn(Completable.complete())
        presenter.attachView(mockView)

        presenter.confirmResetDatabaseSelected()

        verify(mockView).showResetDatabaseSuccess()
    }
}

