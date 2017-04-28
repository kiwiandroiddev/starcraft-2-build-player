package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetSettingsUseCase
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenterTest {

    @Mock lateinit var mockView: BriefView
    @Mock lateinit var mockNavigator: BriefNavigator
    @Mock lateinit var mockGetSettingsUseCase: GetSettingsUseCase

    lateinit var presenter: BriefPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = BriefPresenter(mockGetSettingsUseCase, mockNavigator)

        setUpDefaultMockBehaviour()
    }

    private fun setUpDefaultMockBehaviour() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(true))
    }

    @Test(expected = IllegalStateException::class)
    fun onPlayBuildSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onPlayBuildSelected()
    }

    @Test
    fun onPlayBuildSelected_viewAttached_shouldNavigateToPlayerForBuildId() {
        presenter.attachView(mockView, 1)

        presenter.onPlayBuildSelected()

        verify(mockNavigator).onPlayBuild(1)
    }

    @Test
    fun onPlayBuildSelected_viewAttachedWithDifferentBuildId_shouldNavigateToPlayerForBuildId() {
        presenter.attachView(mockView, 2)

        presenter.onPlayBuildSelected()

        verify(mockNavigator).onPlayBuild(2)
    }

    @Test(expected = IllegalStateException::class)
    fun detachView_afterAttachThenPerformAction_shouldThrowIllegalStateException() {
        presenter.attachView(mockView, 1)

        presenter.detachView()
        presenter.onPlayBuildSelected()
    }

    @Test(expected = IllegalStateException::class)
    fun onEditBuildSelected_noViewAttached_shouldThrowIllegalStateException() {
        presenter.onEditBuildSelected()
    }

    @Test
    fun onEditBuildSelected_viewAttached_shouldNavigateToEditorForBuildId() {
        presenter.attachView(mockView, 1)

        presenter.onEditBuildSelected()

        verify(mockNavigator).onEditBuild(1)
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

        verify(mockView).render(BriefView.BriefViewState(showAds = true))
    }

    @Test
    fun onAttach_showAdSettingOff_tellsViewNotToShowAds() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.just(false))

        presenter.attachView(mockView, 1)

        verify(mockView).render(BriefView.BriefViewState(showAds = false))
    }

    @Test
    fun onAttach_showAdSettingNotEmittedRightAway_rendersWithNoAdsInitially() {
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(Observable.never())

        presenter.attachView(mockView, 1)

        verify(mockView).render(BriefView.BriefViewState(showAds = false))
    }

    @Test
    fun onAttach_newShowAdSettingEmittedLater_viewRendersWithNewState() {
        val showAdsSettingSubject = BehaviorSubject.create<Boolean>()
        `when`(mockGetSettingsUseCase.showAds()).thenReturn(showAdsSettingSubject)
        presenter.attachView(mockView, 1)
        verify(mockView).render(BriefView.BriefViewState(showAds = false))

        showAdsSettingSubject.onNext(true)
        verify(mockView).render(BriefView.BriefViewState(showAds = true))

        showAdsSettingSubject.onNext(false)
        verify(mockView).render(BriefView.BriefViewState(showAds = false))
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

        verify(mockView, times(1)).render(BriefView.BriefViewState(showAds = true))
    }
}

