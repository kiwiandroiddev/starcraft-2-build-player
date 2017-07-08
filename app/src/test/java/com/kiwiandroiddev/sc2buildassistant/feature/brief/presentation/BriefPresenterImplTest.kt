package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import com.kiwiandroiddev.sc2buildassistant.domain.TEST_BUILD
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.*
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewEvent.PlaySelected
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenterImplTest {

    @Mock lateinit var mockView: BriefView
    @Mock lateinit var mockNavigator: BriefNavigator
    @Mock lateinit var mockGetBuildUseCase: GetBuildUseCase
    @Mock lateinit var mockGetSettingsUseCase: GetSettingsUseCase
    @Mock lateinit var mockGetCurrentLanguageUseCase: GetCurrentLanguageUseCase
    @Mock lateinit var mockCheckTranslationPossibleUseCase: CheckTranslationPossibleUseCase
    @Mock lateinit var mockErrorReporter: ErrorReporter

    lateinit var mockViewEventStream: Relay<BriefView.BriefViewEvent>

    lateinit var presenter: BriefPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = BriefPresenterImpl(
                getBuildUseCase = mockGetBuildUseCase,
                getSettingsUseCase = mockGetSettingsUseCase,
                getCurrentLanguageUseCase = mockGetCurrentLanguageUseCase,
                checkTranslationPossibleUseCase = mockCheckTranslationPossibleUseCase,
                navigator = mockNavigator,
                errorReporter = mockErrorReporter,
                postExecutionScheduler = Schedulers.trampoline()
        )

        setUpDefaultMockBehaviour()
    }

    private val DEFAULT_BUILD_ID = 1L

    private fun setUpDefaultMockBehaviour() {
        `when`(mockView.getBuildId()).thenReturn(DEFAULT_BUILD_ID)

        `when`(mockGetBuildUseCase.getBuild(any()))
                .thenReturn(Single.just(TEST_BUILD))

        `when`(mockGetSettingsUseCase.showAds())
                .thenReturn(Observable.just(true))

        mockViewEventStream = PublishRelay.create<BriefView.BriefViewEvent>()

        `when`(mockView.getViewEvents())
                .thenReturn(mockViewEventStream)

        givenUsersCurrentLanguage("en")

        givenTranslatingBetweenAnyLanguageIsPossible()
    }

    private fun givenTranslatingBetweenAnyLanguageIsPossible() {
        `when`(mockCheckTranslationPossibleUseCase.canTranslateFromLanguage(any(), any()))
                .thenReturn(Single.just(true))
    }

    private fun givenUsersCurrentLanguage(languageCode: String) {
        `when`(mockGetCurrentLanguageUseCase.getLanguageCode())
                .thenReturn(Single.just(languageCode))
    }

    private fun givenABuildWithLanguage(languageCode: String?) {
        `when`(mockGetBuildUseCase.getBuild(DEFAULT_BUILD_ID))
                .thenReturn(Single.just(
                        TEST_BUILD.copy(isoLanguageCode = languageCode)
                ))
    }

    private fun givenTranslatingIsPossible(from: String, to: String, isPossible: Boolean) {
        reset(mockCheckTranslationPossibleUseCase)
        `when`(mockCheckTranslationPossibleUseCase.canTranslateFromLanguage(fromLanguageCode = from, toLanguageCode = to))
                .thenReturn(Single.just(isPossible))
    }

    private fun givenCheckingIfTranslationPossibleThrowsError(translateError: RuntimeException) {
        `when`(mockCheckTranslationPossibleUseCase.canTranslateFromLanguage(any(), any()))
                .thenReturn(Single.error(translateError))
    }

    private fun givenGetCurrentLanguageThrowsError(currentLanguageAccessError: RuntimeException) {
        `when`(mockGetCurrentLanguageUseCase.getLanguageCode())
                .thenReturn(Single.error(currentLanguageAccessError))
    }

    @Test
    fun onAttach_showAdSettingOn_tellsViewToShowAds() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(true))

        presenter.attachView(mockView)

        verify(mockView).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingOff_tellsViewNotToShowAds() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(false))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingThrowsError_tellsViewNotToShowAds() {
        `when`(mockGetSettingsUseCase.showAds())
                .thenReturn(Observable.error(IOException("couldn't read ad setting from disk")))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingNotEmittedRightAway_rendersWithNoAdsInitially() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.never())

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })
    }

    @Test
    fun onAttach_newShowAdSettingEmittedLater_viewRendersWithNewState() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView)
        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })

        reset(mockView)
        showAdsSettingSubject.onNext(true)
        verify(mockView).render(argThat { showAds })

        reset(mockView)
        showAdsSettingSubject.onNext(false)
        verify(mockView).render(argThat { !showAds })
    }

    @Test
    fun onDetach_newShowAdSettingEmitted_doesntCrash() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView)

        presenter.detachView()
        showAdsSettingSubject.onNext(true)
    }

    @Test
    fun onDetach_onReattachAndNewShowAdSettingEmitted_onlyOneRenderHappens() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView)

        presenter.detachView()
        presenter.attachView(mockView)
        showAdsSettingSubject.onNext(true)

        verify(mockView, times(1)).render(argThat { showAds })
    }

    @Test
    fun onAttach_noBuildForId_showsBuildLoadErrorInView() {
        `when`(mockGetBuildUseCase.getBuild(DEFAULT_BUILD_ID))
                .thenReturn(Single.error(IOException("couldn't load build")))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_doesNotShowBuildLoadErrorInView() {
        `when`(mockGetBuildUseCase.getBuild(DEFAULT_BUILD_ID))
                .thenReturn(Single.just(TEST_BUILD))

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_rendersBuildBriefInView() {
        `when`(mockGetBuildUseCase.getBuild(DEFAULT_BUILD_ID))
                .thenReturn(Single.just(TEST_BUILD))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat {
                    briefText == TEST_BUILD.notes &&
                            buildSource == TEST_BUILD.source &&
                            buildAuthor == TEST_BUILD.author
                }
        )
    }

    @Test
    fun onPlaySelectedViewEventEmitted_shouldNavigateToPlayer() {
        presenter.attachView(mockView)

        mockViewEventStream.accept(PlaySelected())

        verify(mockNavigator).onPlayBuild(DEFAULT_BUILD_ID)
    }

    @Test
    fun onPlaySelectedViewEventEmitted_differentBuildId_shouldNavigateToPlayer() {
        `when`(mockView.getBuildId()).thenReturn(2L)
        presenter.attachView(mockView)

        mockViewEventStream.accept(PlaySelected())

        verify(mockNavigator).onPlayBuild(2L)
    }

    @Test
    fun onEditBuildEventEmitted_shouldNavigateToEditor() {
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.EditSelected())

        verify(mockNavigator).onEditBuild(DEFAULT_BUILD_ID)
    }

    @Test
    fun onSettingsEventEmitted_shouldNavigateToSettings() {
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.SettingsSelected())

        verify(mockNavigator).onOpenSettings()
    }

    @Test
    fun onAttach_buildLanguageMatchesUserLanguage_translateOptionNotShown() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("en")

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
    }

    @Test
    fun onAttach_buildLanguageDifferentToUserLanguageAndTranslationNotPossible_translateOptionNotShown() {
        givenUsersCurrentLanguage("ru")
        givenABuildWithLanguage("en")
        givenTranslatingIsPossible(from = "en", to = "ru", isPossible = false)

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
    }

    @Test
    fun onAttach_buildLanguageDifferentToUserLanguageAndTranslationPossible_translateOptionShown() {
        givenUsersCurrentLanguage("ru")
        givenABuildWithLanguage("en")
        givenTranslatingIsPossible(from = "en", to = "ru", isPossible = true)

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { showTranslateOption })
    }

    @Test
    fun onAttach_userLanguageUseCaseThrowsError_translateOptionNotShownAndErrorReported() {
        // something's very wrong if we can't even get the device language: report this
        givenABuildWithLanguage("en")
        val currentLanguageAccessError = RuntimeException("could't get user language")
        givenGetCurrentLanguageThrowsError(currentLanguageAccessError)

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showLoadError })
        verify(mockErrorReporter).trackNonFatalError(currentLanguageAccessError)
    }

    @Test
    fun onAttach_checkTranslationPossibleUseCaseThrowsError_translateOptionNotShownAndErrorNotReported() {
        givenABuildWithLanguage("en")
        givenUsersCurrentLanguage("fr")
        givenCheckingIfTranslationPossibleThrowsError(RuntimeException("network down"))

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showLoadError })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onAttach_buildLanguageIsNull_translateOptionNotShown() {
        givenABuildWithLanguage(null)
        givenUsersCurrentLanguage("en")
        givenTranslatingBetweenAnyLanguageIsPossible()

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showLoadError })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }
}

