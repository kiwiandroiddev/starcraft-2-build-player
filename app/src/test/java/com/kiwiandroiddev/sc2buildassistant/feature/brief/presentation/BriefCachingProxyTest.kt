package com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation

import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Created by Matt Clarke on 25/06/17.
 */
class BriefCachingProxyTest {

    @Mock lateinit var mockView: BriefView

    lateinit var briefCachingProxy: BriefCachingProxy

    val TEST_EVENT = BriefView.BriefViewEvent.PlaySelected()
    val TEST_VIEW_STATE = BriefView.BriefViewState(showAds = false, showLoadError = false,
            briefText = null, buildSource = null, buildAuthor = null)

    val TEST_BUILD_ID = 1L

    lateinit var viewEventPublishSubject: PublishSubject<BriefView.BriefViewEvent>

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        briefCachingProxy = BriefCachingProxy(TEST_BUILD_ID)

        viewEventPublishSubject = PublishSubject.create<BriefView.BriefViewEvent>()

        setupMockBehaviors()
    }

    private fun setupMockBehaviors() {
        `when`(mockView.getViewEvents()).thenReturn(Observable.never())
    }

    @Test
    fun render_viewAttached_forwardsRenderCallToAttachedView() {
        briefCachingProxy.attachView(mockView)

        briefCachingProxy.render(TEST_VIEW_STATE)

        verify(mockView).render(TEST_VIEW_STATE)
    }

    @Test
    fun render_viewAttachedThenDetached_doesNotCallRenderOnView() {
        briefCachingProxy.attachView(mockView)
        briefCachingProxy.detachView()

        briefCachingProxy.render(TEST_VIEW_STATE)

        verify(mockView, never()).render(TEST_VIEW_STATE)
    }

    @Test
    fun attachView_renderPreviouslyCalled_viewIsInitialisedWithPriorViewState() {
        briefCachingProxy.render(TEST_VIEW_STATE)

        briefCachingProxy.attachView(mockView)

        verify(mockView).render(TEST_VIEW_STATE)
    }

    @Test
    fun attachView_viewPreviouslyAttachedThenDetached_andRenderCalledWhenViewWasntAttached_renderIsCalledOnViewOnce() {
        briefCachingProxy.attachView(mockView)
        briefCachingProxy.detachView()
        briefCachingProxy.render(TEST_VIEW_STATE)

        briefCachingProxy.attachView(mockView)

        verify(mockView, times(1)).render(TEST_VIEW_STATE)
    }

    @Test(expected = IllegalStateException::class)
    fun getBuildId_noViewHasEverAttached_throwIllegalStateException() {
        briefCachingProxy.getBuildId()
    }

    @Test
    fun getBuildId_viewAttached_returnsBuildIdFromAttachedView() {
        `when`(mockView.getBuildId()).thenReturn(5L)
        briefCachingProxy.attachView(mockView)

        val actualBuildId = briefCachingProxy.getBuildId()

        assertThat(actualBuildId).isEqualTo(5L)
        verify(mockView).getBuildId()
    }

    @Test
    fun getBuildId_viewPreviouslyAttachedThenDetached_returnsPreviouslyAttachedViewsBuildId_andDoesntCallView() {
        `when`(mockView.getBuildId()).thenReturn(10L)
        briefCachingProxy.attachView(mockView)
        briefCachingProxy.detachView()
        reset(mockView)

        val actualBuildId = briefCachingProxy.getBuildId()

        assertThat(actualBuildId).isEqualTo(10L)
        verify(mockView, never()).getBuildId()
    }

    @Test(expected = IllegalStateException::class)
    fun attachView_viewPreviouslyAttachedWithDifferentBuildIdThenDetached_throwsIllegalStateException() {
        `when`(mockView.getBuildId()).thenReturn(1L)
        briefCachingProxy.attachView(mockView)
        briefCachingProxy.detachView()
        reset(mockView)
        `when`(mockView.getBuildId()).thenReturn(2L)
        `when`(mockView.getViewEvents()).thenReturn(Observable.never())

        briefCachingProxy.attachView(mockView)
    }

    @Test
    fun getViewEvents_noViewAttachedSoFar_emitsNothing() {
        briefCachingProxy.getViewEvents().subscribeTestObserver()
                .assertNotTerminated()
                .assertNoValues()
    }

    @Test
    fun getViewEvents_attachedViewEmitsEvent_forwardsViewEvent() {
        `when`(mockView.getViewEvents()).thenReturn(viewEventPublishSubject)
        briefCachingProxy.attachView(mockView)
        val testObserver = briefCachingProxy.getViewEvents().subscribeTestObserver()
        testObserver.assertNoValues().assertNotTerminated()

        viewEventPublishSubject.onNext(TEST_EVENT)

        testObserver.assertValue(TEST_EVENT).assertNotTerminated()
    }

    @Test
    fun getViewEvents_viewAttachesAfterSubscriptionAndEmitsAnEvent_forwardsViewEvent() {
        val testObserver = briefCachingProxy.getViewEvents().subscribeTestObserver()
        `when`(mockView.getViewEvents()).thenReturn(viewEventPublishSubject)
        briefCachingProxy.attachView(mockView)

        viewEventPublishSubject.onNext(TEST_EVENT)

        testObserver.assertValue(TEST_EVENT).assertNotTerminated()
    }

    @Test
    fun getViewEvents_viewEmitsEventAfterItsDetached_doesNotForwardEvent() {
        val testObserver = briefCachingProxy.getViewEvents().subscribeTestObserver()
        testObserver.assertNoValues().assertNotTerminated()

        `when`(mockView.getViewEvents()).thenReturn(viewEventPublishSubject)
        briefCachingProxy.attachView(mockView)
        briefCachingProxy.detachView()

        viewEventPublishSubject.onNext(TEST_EVENT)

        testObserver.assertNoValues().assertNotTerminated()
    }

}