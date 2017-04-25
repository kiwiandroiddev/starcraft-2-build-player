package com.kiwiandroiddev.sc2buildassistant.domain

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.BuildPlayer
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.BuildPlayerEventListener
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.CurrentTimeProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Created by matt on 23/03/17.
 */
class BuildPlayerTest {

    companion object {
        val TEST_ITEM_1 = BuildItem(15, "probe")
        val TEST_ITEM_2 = BuildItem(140, "pylon")
    }

    @Mock lateinit var mockBuildPlayerEventListener: BuildPlayerEventListener
    @Mock lateinit var mockCurrentTimeProvider: CurrentTimeProvider

    lateinit var player: BuildPlayer

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        player = BuildPlayer(mockCurrentTimeProvider, emptyList())
        player.alertOffsetInGameSeconds = 0

        initDefaultMockBehaviours()
    }

    private fun initDefaultMockBehaviours() {
        `when`(mockCurrentTimeProvider.time).thenReturn(0L)
    }

    private fun initPlayerWithItems(items: List<BuildItem>) {
        player = BuildPlayer(mockCurrentTimeProvider, items)
        player.setListener(mockBuildPlayerEventListener)
    }

    private fun setCurrentTimeToGameSeconds(gameSeconds: Int) {
        `when`(mockCurrentTimeProvider.time).thenReturn(gameSeconds * 1000L)
    }

    @Test
    fun getDuration_noBuildItems_durationIsZero() {
        assertThat(player.duration).isEqualTo(0)
    }

    @Test
    fun getDuration_2buildItems_durationIsTimeOfLastItem() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2
        ))

        val duration = player.duration

        assertThat(duration).isEqualTo(TEST_ITEM_2.time)
    }

    @Test
    fun isPlaying_playerJustInitialised_shouldBeFalse() {
        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun isPlaying_playCalled_shouldBeTrue() {
        player.play()

        assertThat(player.isPlaying).isTrue()
    }

    @Test
    fun isPlaying_playThenPauseCalled_shouldBeFalse() {
        player.play()
        player.pause()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun isPlaying_playThenStoppedCalled_shouldBeFalse() {
        player.play()
        player.stop()

        assertThat(player.isPlaying).isFalse()
    }

    @Test
    fun registerListener_alreadyPlaying_listenerInitialisedWithOnPlayEvent() {
        player.play()

        player.setListener(mockBuildPlayerEventListener)

        verify(mockBuildPlayerEventListener).onBuildPlay()
    }

    @Test
    fun registerListener_alreadyPaused_listenerInitialisedWithOnPausedEvent() {
        player.play()
        player.pause()

        player.setListener(mockBuildPlayerEventListener)

        verify(mockBuildPlayerEventListener).onBuildPaused()
    }

    @Test
    fun registerListener_playerStopped_listenerInitialisedWithOnStoppedEvent() {
        player.setListener(mockBuildPlayerEventListener)

        verify(mockBuildPlayerEventListener).onBuildStopped()
    }

    @Test
    fun clearListener_playbackStartsAfterListenerCleared_listenerDoesNotReceiveEvent() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2
        ))

        player.clearListener()
        player.play()
        player.iterate()

        verify(mockBuildPlayerEventListener, never()).onBuildPlay()
    }

    @Test
    fun playThenStop_buildPlayerStateShouldBeTheSameAsBeforePlaybackStarted() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2
        ))
        val stateBeforePlay = player.toString()

        player.play()
        player.iterate()
        player.stop()
        player.iterate()

        val stateAfterStop = player.toString()
        assertThat(stateAfterStop).isEqualTo(stateBeforePlay)
    }

    @Test
    fun iterate_pastTimeOfFirstItem_listenerIsNotifiedToBuildFirstItem() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))

        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(30)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(TEST_ITEM_1, 0)
    }

    @Test
    fun iterate_pastTimeOfSecondItem_listenerIsNotifiedToBuildSecondItemAtCorrectPosition() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))

        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(TEST_ITEM_2.time)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(TEST_ITEM_2, 1)
    }

    @Test
    fun iterate_zeroAlertTimeOffsetAndAtTimeOfFirstItem_listenerIsNotifiedToBuildFirstItem() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.alertOffsetInGameSeconds = 0

        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(15)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(TEST_ITEM_1, 0)
    }

    @Test
    fun iterate_fiveSecondsAlertTimeOffsetAndAtSixSecondsBeforeFirstItem_listenerIsNotNotifiedToBuildFirstItem() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.alertOffsetInGameSeconds = 5

        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(9)

        player.iterate()

        verify(mockBuildPlayerEventListener, never()).onBuildThisNow(TEST_ITEM_1, 0)
    }

    @Test
    fun setBuildItemFilter_filterOutEverything_durationIsZero() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2))

        player.buildItemFilter = { _ -> false }

        assertThat(player.duration).isEqualTo(0)
    }

    @Test
    fun clearBuildItemFilter_initiallyFiltersOutEverything_durationIsTimeOfLastItem() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2))
        player.buildItemFilter = { _ -> false }

        player.clearBuildItemFilter()

        assertThat(player.duration).isEqualTo(140)
    }

    @Test
    fun setBuildItemFilter_filterNothing_durationIsTimeOfLastItem() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2))

        player.buildItemFilter = { _ -> true }

        assertThat(player.duration).isEqualTo(140)
    }

    @Test
    fun setBuildItemFilter_filterLastItem_durationIsTimeOfSecondToLastItem() {
        initPlayerWithItems(listOf(
                TEST_ITEM_1,
                TEST_ITEM_2))

        player.buildItemFilter = { item -> item.gameItemID != TEST_ITEM_2.gameItemID }

        assertThat(player.duration).isEqualTo(15)
    }

    @Test
    fun iterate_pastTimeOfFilteredFirstItem_listenerIsNotNotifiedToBuildFirstItem() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.buildItemFilter = { item -> item.gameItemID != TEST_ITEM_1.gameItemID }
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(15)

        player.iterate()

        verify(mockBuildPlayerEventListener, never()).onBuildThisNow(TEST_ITEM_1, 0)
    }

    @Test
    fun iterate_pastTimeOfSecondItemAndFirstItemFiltered_listenerIsNotifiedToBuildSecondItemAtIndexZero() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.buildItemFilter = { item -> item != TEST_ITEM_1 }
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(150)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(TEST_ITEM_2, 0)
    }

    @Test
    fun iterate_filterChangedSinceLastIteration_listenerNotifiedThatBuildItemsHaveChanged() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(5)
        reset(mockBuildPlayerEventListener)

        player.buildItemFilter = { item -> item.gameItemID != TEST_ITEM_1.gameItemID }
        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildItemsChanged(listOf(TEST_ITEM_2))
    }

    @Test
    fun iterate_filterChangedAndListenerAlreadyNotified_listenerNotNotifiedAgain() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(5)
        reset(mockBuildPlayerEventListener)

        player.buildItemFilter = { item -> item.gameItemID != TEST_ITEM_1.gameItemID }
        player.iterate()
        player.iterate()

        verify(mockBuildPlayerEventListener, times(1)).onBuildItemsChanged(listOf(TEST_ITEM_2))
    }

    @Test
    fun iterate_filterClearedSinceLastIteration_listenerNotifiedThatBuildItemsHaveChanged() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.buildItemFilter = { _ -> false }
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(5)
        reset(mockBuildPlayerEventListener)

        player.clearBuildItemFilter()
        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildItemsChanged(listOf(TEST_ITEM_1, TEST_ITEM_2))
    }

    @Test
    fun iterate_newBlanketFilterMeansBuildIsFinished_listenerIsNotifiedThatBuildIsFinished() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(30)
        player.iterate()
        reset(mockBuildPlayerEventListener)

        player.buildItemFilter = { _ -> false }
        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildFinished()
    }

    @Test
    fun iterate_newSelectiveFilterMeansBuildIsFinished_listenerIsNotifiedThatBuildIsFinished() {
        initPlayerWithItems(listOf(TEST_ITEM_1, TEST_ITEM_2))
        player.play()
        player.iterate()
        setCurrentTimeToGameSeconds(30)
        player.iterate()
        reset(mockBuildPlayerEventListener)

        player.buildItemFilter = { it != TEST_ITEM_2 }
        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildFinished()
    }

}