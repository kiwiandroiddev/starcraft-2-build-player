package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import com.nhaarman.mockito_kotlin.argThat
import io.reactivex.Observable
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
class BriefPresenterTest {

    companion object {
        val TEST_BUILD = Build().apply { notes = "Build instructions here" }
    }

    @Mock lateinit var mockView: BriefView
    @Mock lateinit var mockNavigator: BriefNavigator
    @Mock lateinit var mockGetBuildUseCase: GetBuildUseCase
    @Mock lateinit var mockGetSettingsUseCase: GetSettingsUseCase

    lateinit var presenter: BriefPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = BriefPresenter(mockGetBuildUseCase, mockGetSettingsUseCase, mockNavigator)

        setUpDefaultMockBehaviour()
    }

    private fun setUpDefaultMockBehaviour() {
        `when`(mockGetBuildUseCase.getBuild(com.nhaarman.mockito_kotlin.any()))
                .thenReturn(Observable.just(TEST_BUILD))
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(true))
    }

    @Test(expected = IllegalStateException::class)
    fun onPlayBuildSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onPlayBuildSelected()
    }

    @Test
    fun onPlayBuildSelected_viewAttached_shouldNavigateToPlayerForBuildId() {
        presenter.attachView(view = mockView, buildId = 1)

        presenter.onPlayBuildSelected()

        verify(mockNavigator).onPlayBuild(buildId = 1)
    }

    @Test
    fun onPlayBuildSelected_viewAttachedWithDifferentBuildId_shouldNavigateToPlayerForBuildId() {
        presenter.attachView(view = mockView, buildId = 2)

        presenter.onPlayBuildSelected()

        verify(mockNavigator).onPlayBuild(buildId = 2)
    }

    @Test(expected = IllegalStateException::class)
    fun detachView_afterAttachThenPerformAction_shouldThrowIllegalStateException() {
        presenter.attachView(view = mockView, buildId = 1)

        presenter.detachView()
        presenter.onPlayBuildSelected()
    }

    @Test(expected = IllegalStateException::class)
    fun onEditBuildSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onEditBuildSelected()
    }

    @Test
    fun onEditBuildSelected_viewAttached_shouldNavigateToEditorForBuildId() {
        presenter.attachView(view = mockView, buildId = 1)

        presenter.onEditBuildSelected()

        verify(mockNavigator).onEditBuild(buildId = 1)
    }

    @Test(expected = IllegalStateException::class)
    fun onSettingsSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onSettingsSelected()
    }

    @Test
    fun onSettingsSelected_viewAttached_shouldNavigateToOpenSettings() {
        presenter.attachView(mockView, 2)

        presenter.onSettingsSelected()

        verify(mockNavigator).onOpenSettings()
    }

    @Test
    fun onAttach_showAdSettingOn_tellsViewToShowAds() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(true))

        presenter.attachView(mockView, 1)

        verify(mockView).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingOff_tellsViewNotToShowAds() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(false))

        presenter.attachView(mockView, 1)

        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingThrowsError_tellsViewNotToShowAds() {
        `when`(mockGetSettingsUseCase.showAds())
                .thenReturn(Observable.error(IOException("couldn't read ad setting from disk")))

        presenter.attachView(mockView, 1)

        verify(mockView, atLeastOnce()).render(argThat { !showAds })
        verify(mockView, never()).render(argThat { showAds })
    }

    @Test
    fun onAttach_showAdSettingNotEmittedRightAway_rendersWithNoAdsInitially() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.never())

        presenter.attachView(mockView, 1)

        verify(mockView).render(argThat { !showAds })
    }

    @Test
    fun onAttach_newShowAdSettingEmittedLater_viewRendersWithNewState() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView, 1)
        verify(mockView).render(argThat { !showAds })

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
        presenter.attachView(mockView, 1)

        presenter.detachView()
        showAdsSettingSubject.onNext(true)
    }

    @Test
    fun onDetach_onReattachAndNewShowAdSettingEmitted_onlyOneRenderHappens() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView, 1)

        presenter.detachView()
        presenter.attachView(mockView, 1)
        showAdsSettingSubject.onNext(true)

        verify(mockView, times(1)).render(argThat { showAds })
    }

    @Test
    fun onAttach_noBuildForId_showsBuildLoadErrorInView() {
        `when`(mockGetBuildUseCase.getBuild(1))
                .thenReturn(Observable.error(IOException("couldn't load build")))

        presenter.attachView(mockView, 1)

        verify(mockView, atLeastOnce()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_doesNotShowBuildLoadErrorInView() {
        `when`(mockGetBuildUseCase.getBuild(1))
                .thenReturn(Observable.just(TEST_BUILD))

        presenter.attachView(mockView, 1)

        verify(mockView, never()).render(argThat { showLoadError })
    }

    @Test
    fun onAttach_haveBuildForId_rendersBuildBriefInView() {
        `when`(mockGetBuildUseCase.getBuild(1))
                .thenReturn(Observable.just(TEST_BUILD))

        presenter.attachView(mockView, 1)

        verify(mockView).render(BriefView.BriefViewState(true, false, briefText = TEST_BUILD.notes))
    }

}
