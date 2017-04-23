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

    @Mock lateinit var mockBuildPlayerEventListener: BuildPlayerEventListener
    @Mock lateinit var mockCurrentTimeProvider: CurrentTimeProvider

    lateinit var player: BuildPlayer

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        player = BuildPlayer(mockCurrentTimeProvider, emptyList())
    }

    private fun initPlayerWithItems(items: List<BuildItem>) {
        player = BuildPlayer(mockCurrentTimeProvider, items)
        player.registerListener(mockBuildPlayerEventListener)
    }

    @Test
    fun getDuration_noBuildItems_durationIsZero() {
        assertThat(player.duration).isEqualTo(0)
    }

    @Test
    fun getDuration_2buildItems_durationIsTimeOfLastItem() {
        initPlayerWithItems(listOf(
                BuildItem(120, "pylon"),
                BuildItem(150, "gateway")
        ))

        val duration = player.duration

        assertThat(duration).isEqualTo(150)
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
    fun playThenStop_buildPlayerStateShouldBeTheSameAsBeforePlaybackStarted() {
        initPlayerWithItems(listOf(
                BuildItem(120, "pylon"),
                BuildItem(150, "gateway")
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
        val firstItem = BuildItem(15, "probe")
        val secondItem = BuildItem(140, "pylon")
        initPlayerWithItems(listOf(firstItem, secondItem))

        `when`(mockCurrentTimeProvider.time).thenReturn(0L)
        player.play()
        player.iterate()
        `when`(mockCurrentTimeProvider.time).thenReturn(30 * 1000L)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(firstItem, 0)
    }

    @Test
    fun iterate_zeroAlertTimeOffsetAndAtTimeOfFirstItem_listenerIsNotifiedToBuildFirstItem() {
        val firstItem = BuildItem(15, "probe")
        val secondItem = BuildItem(140, "pylon")
        initPlayerWithItems(listOf(firstItem, secondItem))
        player.alertOffset = 0

        `when`(mockCurrentTimeProvider.time).thenReturn(0L)
        player.play()
        player.iterate()
        `when`(mockCurrentTimeProvider.time).thenReturn(15 * 1000L)

        player.iterate()

        verify(mockBuildPlayerEventListener).onBuildThisNow(firstItem, 0)
    }

    @Test
    fun iterate_fiveSecondsAlertTimeOffsetAndAtSixSecondsBeforeFirstItem_listenerIsNotNotifiedToBuildFirstItem() {
        val firstItem = BuildItem(15, "probe")
        val secondItem = BuildItem(140, "pylon")
        initPlayerWithItems(listOf(firstItem, secondItem))
        player.alertOffset = 5

        `when`(mockCurrentTimeProvider.time).thenReturn(0L)
        player.play()
        player.iterate()
        `when`(mockCurrentTimeProvider.time).thenReturn(9 * 1000L)

        player.iterate()

        verify(mockBuildPlayerEventListener, never()).onBuildThisNow(firstItem, 0)
    }

}