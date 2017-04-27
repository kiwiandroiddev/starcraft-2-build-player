package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

/**
 * Created by Matt Clarke on 28/04/17.
 */
class BriefPresenterTest {

    @Mock lateinit var mockView: BriefView
    @Mock lateinit var mockNavigator: BriefNavigator

    lateinit var presenter: BriefPresenter

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        presenter = BriefPresenter(mockNavigator)
    }

    @Test(expected = IllegalStateException::class)
    fun onPlayBuildSelected_noAttachedView_shouldThrowIllegalArgumentException() {
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
}
