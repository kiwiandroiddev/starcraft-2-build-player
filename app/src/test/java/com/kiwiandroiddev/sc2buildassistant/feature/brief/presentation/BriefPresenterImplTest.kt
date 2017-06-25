package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.jakewharton.rxrelay2.PublishRelay
import com.jakewharton.rxrelay2.Relay
import com.kiwiandroiddev.sc2buildassistant.domain.TEST_BUILD
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefView.BriefViewEvent.PlaySelected
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.GetSettingsUseCase
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

    lateinit var mockViewEventStream: Relay<BriefView.BriefViewEvent>

    lateinit var presenter: BriefPresenterImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = BriefPresenterImpl(
                getBuildUseCase = mockGetBuildUseCase,
                getSettingsUseCase = mockGetSettingsUseCase,
                navigator = mockNavigator,
                postExecutionScheduler = Schedulers.trampoline()
        )

        setUpDefaultMockBehaviour()
    }

    private fun setUpDefaultMockBehaviour() {
        `when`(mockView.getBuildId()).thenReturn(1)

        `when`(mockGetBuildUseCase.getBuild(com.nhaarman.mockito_kotlin.any()))
                .thenReturn(Single.just(TEST_BUILD))

        `when`(mockGetSettingsUseCase.showAds())
                .thenReturn(Observable.just(true))

        mockViewEventStream = PublishRelay.create<BriefView.BriefViewEvent>()

        `when`(mockView.getViewEvents())
                .thenReturn(mockViewEventStream)
    }

    @Test(expected = IllegalStateException::class)
    fun onEditBuildSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onEditBuildSelected()
    }

    @Test
    fun onEditBuildSelected_viewAttached_shouldNavigateToEditorForBuildId() {
        `when`(mockView.getBuildId()).thenReturn(1)
        presenter.attachView(view = mockView)

        presenter.onEditBuildSelected()

        verify(mockNavigator).onEditBuild(buildId = 1)
    }

    @Test(expected = IllegalStateException::class)
    fun onSettingsSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onSettingsSelected()
    }

    @Test
    fun onSettingsSelected_viewAttached_shouldNavigateToOpenSettings() {
        `when`(mockView.getBuildId()).thenReturn(2)
        presenter.attachView(mockView)

        presenter.onSettingsSelected()

        verify(mockNavigator).onOpenSettings()
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
        `when`(mockGetBuildUseCase.getBuild(1))
                .thenReturn(Single.error(IOException("couldn't load build")))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_doesNotShowBuildLoadErrorInView() {
        `when`(mockGetBuildUseCase.getBuild(1))
                .thenReturn(Single.just(TEST_BUILD))

        presenter.attachView(mockView)

        verify(mockView, never()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_rendersBuildBriefInView() {
        `when`(mockGetBuildUseCase.getBuild(1L))
                .thenReturn(Single.just(TEST_BUILD))

        presenter.attachView(mockView)

        verify(mockView, atLeastOnce()).render(
                BriefView.BriefViewState(
                        showAds = true,
                        showLoadError = false,
                        briefText = TEST_BUILD.notes,
                        buildSource = TEST_BUILD.source,
                        buildAuthor = TEST_BUILD.author
                )
        )
    }

    @Test
    fun onAttach_onPlaySelectedViewEventEmitted_shouldNavigateToPlayer() {
        presenter.attachView(mockView)

        mockViewEventStream.accept(PlaySelected())

        verify(mockNavigator).onPlayBuild(1L)
    }

    @Test
    fun onAttach_differentBuildId_onPlaySelectedViewEventEmitted_shouldNavigateToPlayer() {
        `when`(mockView.getBuildId()).thenReturn(2L)
        presenter.attachView(mockView)

        mockViewEventStream.accept(PlaySelected())

        verify(mockNavigator).onPlayBuild(2L)
    }

}
