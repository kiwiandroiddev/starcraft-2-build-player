package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import com.kiwiandroiddev.sc2buildassistant.domain.TEST_BUILD
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetCurrentLanguageUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.ShouldTranslateBuildByDefaultUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.GetTranslationUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.*
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewEvent.PlaySelected
import com.kiwiandroiddev.sc2buildassistant.feature.errorreporter.ErrorReporter
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.SingleSubject
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
    @Mock lateinit var mockGetTranslationUseCase: GetTranslationUseCase
    @Mock lateinit var mockShouldTranslateBuildByDefaultUseCase: ShouldTranslateBuildByDefaultUseCase
    @Mock lateinit var mockErrorReporter: ErrorReporter

    lateinit var mockViewEventStream: Relay<BriefView.BriefViewEvent>

    lateinit var presenter: BriefPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // Force background operations onto the main thread to make tests run synchronously
        RxJavaPlugins.setIoSchedulerHandler { Schedulers.trampoline() }

        presenter = BriefPresenterImpl(
                getBuildUseCase = mockGetBuildUseCase,
                getSettingsUseCase = mockGetSettingsUseCase,
                getCurrentLanguageUseCase = mockGetCurrentLanguageUseCase,
                checkTranslationPossibleUseCase = mockCheckTranslationPossibleUseCase,
                shouldTranslateBuildByDefaultUseCase = mockShouldTranslateBuildByDefaultUseCase,
                getTranslationUseCase = mockGetTranslationUseCase,
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

        givenTranslateByDefaultOptionForBuildIs(false)
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

    private fun givenABuildWithLanguageAndBriefText(languageCode: String?, brief: String?) {
        `when`(mockGetBuildUseCase.getBuild(DEFAULT_BUILD_ID))
                .thenReturn(Single.just(
                        TEST_BUILD.copy(
                                isoLanguageCode = languageCode,
                                notes = brief
                        )
                ))
    }

    private fun givenTranslatingIsPossible(from: String, to: String, isPossible: Boolean) {
        reset(mockCheckTranslationPossibleUseCase)
        `when`(mockCheckTranslationPossibleUseCase.canTranslateFromLanguage(from, to))
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

    private fun givenGetTranslationAlwaysThrowsAnError(translationError: RuntimeException) {
        `when`(mockGetTranslationUseCase.getTranslation(fromLanguageCode = any(),
                toLanguageCode = any(), sourceText = any()))
                .thenReturn(Single.error(translationError))
    }

    private fun givenGetTranslationWillNeverComplete() {
        `when`(mockGetTranslationUseCase.getTranslation(fromLanguageCode = any(),
                toLanguageCode = any(), sourceText = any())).thenReturn(Single.never())
    }

    private fun givenTranslationOptionAvailableForBuildAndWillSucceed(
            withResult: String = "Senden Sie SCVs an die feindliche Basis") {
        givenUsersCurrentLanguage("de")
        givenABuildWithLanguageAndBriefText("en", "Send starting SCVs to enemy base")
        givenTranslatingIsPossible(from = "en", to = "de", isPossible = true)
        `when`(mockGetTranslationUseCase.getTranslation(
                fromLanguageCode = "en",
                toLanguageCode = "de",
                sourceText = "Send starting SCVs to enemy base"))
                .thenReturn(Single.just(withResult))
    }

    private fun givenABuildInADifferentLanguage() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguageAndBriefText("fr", "Envoyer des SCV de départ à la base ennemie")
    }

    private fun givenTranslateByDefaultOptionForBuildIs(translateByDefault: Boolean) {
        `when`(mockShouldTranslateBuildByDefaultUseCase.shouldTranslateByDefault(DEFAULT_BUILD_ID))
                .thenReturn(Single.just(translateByDefault))
    }

    private fun givenABuildInUsersNativeLanguage() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("en")
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

    @Test
    fun onAttach_buildNotesIsNull_translateOptionNotShown() {
        givenABuildWithLanguageAndBriefText(languageCode = "en", brief = null)
        givenUsersCurrentLanguage("de")
        givenTranslatingBetweenAnyLanguageIsPossible()

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showLoadError })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onAttach_revertTranslationOption_neverShown() {
        // builds always start off untranslated (at this stage)
        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockView, atLeastOnce()).render(argThat { !showRevertTranslationOption })
    }

    @Test
    fun onTranslateViewEventEmitted_translationUseCaseThrowsError_shouldShowErrorInView() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("ru")
        givenTranslatingIsPossible(from = "ru", to = "en", isPossible = true)
        givenGetTranslationAlwaysThrowsAnError(RuntimeException("can't access network translation service"))
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onTranslateViewEventEmitted_buildAndUserLanguageMatch_showsTranslateErrorAndReports() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("en")
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockErrorReporter).trackNonFatalError(argThat {
            (this is IllegalStateException && message == "Translate selected when translation not available or needed")
        })
    }

    @Test
    fun onTranslateViewEventEmitted_translationNotPossible_showsTranslateErrorAndReports() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("ru")
        givenTranslatingIsPossible(from = "ru", to = "en", isPossible = false)
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockErrorReporter).trackNonFatalError(argThat {
            (this is IllegalStateException && message == "Translate selected when translation not available or needed")
        })
    }

    @Test
    fun onTranslateViewEventEmitted_translationUseCaseWontComplete_shouldShowTranslationLoadingInView() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("ru")
        givenTranslatingIsPossible(from = "ru", to = "en", isPossible = true)
        givenGetTranslationWillNeverComplete()
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView, never()).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onTranslateViewEventEmitted_translationUseCaseWillEventuallyFail_shouldHideTranslationLoadingAfterItFails() {
        givenUsersCurrentLanguage("en")
        givenABuildWithLanguage("ru")
        givenTranslatingIsPossible(from = "ru", to = "en", isPossible = true)
        val translationResultSubject = SingleSubject.create<String>()
        `when`(mockGetTranslationUseCase.getTranslation(fromLanguageCode = any(),
                toLanguageCode = any(), sourceText = any())).thenReturn(translationResultSubject)
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView, never()).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })

        reset(mockView)
        translationResultSubject.onError(RuntimeException("Couldn't access translation service"))

        verify(mockView).render(argThat { showTranslationError })
        verify(mockView, never()).render(argThat { translationLoading })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onTranslateViewEventEmitted_translationFromEnToDeWillSucceed_shouldShowTranslatedBriefAndHideLoading() {
        val GERMAN_TRANSLATION = "Senden Sie SCVs an die feindliche Basis"
        givenTranslationOptionAvailableForBuildAndWillSucceed(withResult = GERMAN_TRANSLATION)
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView, never()).render(argThat { showTranslationError })
        verify(mockView, atLeastOnce()).render(argThat {
            briefText == GERMAN_TRANSLATION && !translationLoading && !showTranslationError
        })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onTranslateViewEventEmitted_translationFromEnToDeWillSucceed_shouldShowRevertTranslationAndHideTranslateOption() {
        givenTranslationOptionAvailableForBuildAndWillSucceed()
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { !showTranslateOption && showRevertTranslationOption })
    }

    @Test
    fun onTranslateViewEventEmitted_translationFromFrToEnWillSucceed_shouldShowTranslatedBriefAndHideLoading() {
        givenABuildInADifferentLanguage()
        givenTranslatingBetweenAnyLanguageIsPossible()
        `when`(mockGetTranslationUseCase.getTranslation(
                fromLanguageCode = any(),
                toLanguageCode = any(),
                sourceText = any()))
                .thenReturn(Single.just("Send starting SCVs to enemy base"))
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView, never()).render(argThat { showTranslationError })
        verify(mockView, atLeastOnce()).render(argThat {
            briefText == "Send starting SCVs to enemy base" &&
                    !translationLoading && !showTranslationError
        })
        verify(mockErrorReporter, never()).trackNonFatalError(any())
    }

    @Test
    fun onTranslateViewEvent_translateUseCaseThrowsError_shouldShowTranslationErrorInViewAndHideLoading() {
        givenABuildInADifferentLanguage()
        givenTranslatingBetweenAnyLanguageIsPossible()
        `when`(mockGetTranslationUseCase.getTranslation(
                fromLanguageCode = any(),
                toLanguageCode = any(),
                sourceText = any()))
                .thenReturn(Single.error(RuntimeException("network error!")))
        presenter.attachView(mockView)

        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())

        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView).render(argThat { showTranslationError })
    }

    // TODO refactor presenter to improve readability
    // TODO refactor tests for readability
    // TODO convert failed non-nullable calls (!!) to errors in presenter

    @Test
    fun onRevertTranslationViewEvent_translationStillPossible_hideRevertTranslationOptionAndShowsTranslateOption() {
        givenTranslationOptionAvailableForBuildAndWillSucceed()
        presenter.attachView(mockView)
        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())
        reset(mockView)
        `when`(mockView.getBuildId()).thenReturn(DEFAULT_BUILD_ID)

        mockViewEventStream.accept(BriefViewEvent.RevertTranslationSelected())

        verify(mockView, atLeastOnce()).render(argThat { !showRevertTranslationOption && showTranslateOption })
        verify(mockView, never()).render(argThat { showTranslationError })
    }

    @Test
    fun onRevertTranslationViewEvent_translationNoLongerPossible_hideBothRevertTranslationOptionAndTranslateOption() {
        givenTranslationOptionAvailableForBuildAndWillSucceed()
        presenter.attachView(mockView)
        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())
        givenTranslatingIsPossible(from = "en", to = "de", isPossible = false)
        reset(mockView)
        `when`(mockView.getBuildId()).thenReturn(DEFAULT_BUILD_ID)

        mockViewEventStream.accept(BriefViewEvent.RevertTranslationSelected())

        verify(mockView, atLeastOnce()).render(argThat { !showRevertTranslationOption && !showTranslateOption })
        verify(mockView, never()).render(argThat { showTranslationError })
    }

    @Test
    fun onRevertTranslationViewEvent_untranslatedBuildNotesShownInView() {
        val ORIGINAL_BRIEF_TEXT = "Send starting SCVs to enemy base"
        givenUsersCurrentLanguage("de")
        givenABuildWithLanguageAndBriefText("en", ORIGINAL_BRIEF_TEXT)
        givenTranslatingIsPossible(from = "en", to = "de", isPossible = true)
        `when`(mockGetTranslationUseCase.getTranslation(
                fromLanguageCode = "en",
                toLanguageCode = "de",
                sourceText = ORIGINAL_BRIEF_TEXT))
                .thenReturn(Single.just("Senden Sie SCVs an die feindliche Basis"))
        presenter.attachView(mockView)
        mockViewEventStream.accept(BriefViewEvent.TranslateSelected())
        reset(mockView)
        `when`(mockView.getBuildId()).thenReturn(DEFAULT_BUILD_ID)
        givenABuildWithLanguageAndBriefText("en", ORIGINAL_BRIEF_TEXT)

        mockViewEventStream.accept(BriefViewEvent.RevertTranslationSelected())

        verify(mockView, atLeastOnce()).render(argThat { briefText == ORIGINAL_BRIEF_TEXT })
    }

    @Test
    fun onAttachView_buildInDifferentLanguageAndNoTranslationPreferenceForBuild_briefNotTranslatedByDefault() {
        givenABuildInADifferentLanguage()
        givenTranslateByDefaultOptionForBuildIs(false)
        givenTranslatingBetweenAnyLanguageIsPossible()

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockGetTranslationUseCase, never()).getTranslation(any(), any(), any())
    }

    @Test
    fun onAttachView_buildInUsersLanguageAndHaveTranslateByDefaultPreferenceForBuild_briefNotTranslated() {
        givenABuildInUsersNativeLanguage()
        givenTranslateByDefaultOptionForBuildIs(true)
        givenTranslatingBetweenAnyLanguageIsPossible()

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, never()).render(argThat { showRevertTranslationOption })
        verify(mockGetTranslationUseCase, never()).getTranslation(any(), any(), any())
    }

    @Test
    fun onAttachView_buildInDifferentLanguageAndHaveTranslateByDefaultPreferenceForBuild_briefTranslatedByDefault() {
        givenABuildInADifferentLanguage()
        givenTranslateByDefaultOptionForBuildIs(true)
        givenTranslationOptionAvailableForBuildAndWillSucceed(withResult = "Train 5 zerglings")

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showTranslateOption })
        verify(mockView, atLeastOnce()).render(argThat { translationLoading })
        verify(mockView, atLeastOnce()).render(argThat { showRevertTranslationOption })
        verify(mockGetTranslationUseCase).getTranslation(any(), any(), any())
        verify(mockView, atLeastOnce()).render(argThat { briefText == "Train 5 zerglings" })
    }

    // TODO show loading banner initially incase cache is empty

}

