package com.kiwiandroiddev.sc2buildassistant.domain;

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.BuildPlayer;
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.BuildPlayerEventListener;
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.CurrentTimeProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by matt on 23/03/17.
 */
public class BuildPlayerTest {

    BuildPlayer player;

    @Mock
    BuildPlayerEventListener mockBuildPlayerEventListener;
    @Mock
    CurrentTimeProvider mockCurrentTimeProvider;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        player = new BuildPlayer(mockCurrentTimeProvider, new ArrayList<BuildItem>());
    }

    @Test
    public void playThenStop_buildPlayerStateShouldBeTheSameAsBeforePlaybackStarted() throws Exception {
        ArrayList<BuildItem> items = new ArrayList<>(Arrays.asList(
                new BuildItem(120, "pylon"),
                new BuildItem(150, "gateway")
        ));
        BuildPlayer player = new BuildPlayer(mockCurrentTimeProvider, items);
        String stateBeforePlay = player.toString();

        player.play();
        player.iterate();
        player.stop();
        player.iterate();

        String stateAfterStop = player.toString();
        assertThat(stateAfterStop).isEqualTo(stateBeforePlay);
    }

    @Test
    public void getDuration_2buildItems_durationIsTimeOfLastItem() throws Exception {
        ArrayList<BuildItem> items = new ArrayList<>(Arrays.asList(
                new BuildItem(120, "pylon"),
                new BuildItem(150, "gateway")
        ));
        player = new BuildPlayer(mockCurrentTimeProvider, items);

        int duration = player.getDuration();

        assertThat(duration).isEqualTo(150);
    }

    @Test
    public void getDuration_noBuildItems_durationIsZero() throws Exception {
        int duration = player.getDuration();

        assertThat(duration).isEqualTo(0);
    }

    @Test
    public void isPlaying_playerJustInitialised_shouldBeFalse() throws Exception {
        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void isPlaying_playCalled_shouldBeTrue() throws Exception {
        player.play();

        assertThat(player.isPlaying()).isTrue();
    }

    @Test
    public void isPlaying_playThenPauseCalled_shouldBeFalse() throws Exception {
        player.play();
        player.pause();

        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void isPlaying_playThenStoppedCalled_shouldBeFalse() throws Exception {
        player.play();
        player.stop();

        assertThat(player.isPlaying()).isFalse();
    }

    @Test
    public void iterate_pastTimeOfFirstItem_listenerIsNotifiedToBuildFirstItem() throws Exception {
        BuildItem firstItem = new BuildItem(15, "probe");
        BuildItem secondItem = new BuildItem(140, "pylon");
        initPlayerWithItems(Arrays.asList(firstItem, secondItem));

        when(mockCurrentTimeProvider.getTime()).thenReturn(0L);
        player.play();
        player.iterate();
        when(mockCurrentTimeProvider.getTime()).thenReturn(30 * 1000L);

        player.iterate();

        verify(mockBuildPlayerEventListener).onBuildThisNow(firstItem, 0);
    }

    @Test
    public void iterate_zeroAlertTimeOffsetAndAtTimeOfFirstItem_listenerIsNotifiedToBuildFirstItem() throws Exception {
        BuildItem firstItem = new BuildItem(15, "probe");
        BuildItem secondItem = new BuildItem(140, "pylon");
        initPlayerWithItems(Arrays.asList(firstItem, secondItem));
        player.setAlertOffset(0);

        when(mockCurrentTimeProvider.getTime()).thenReturn(0L);
        player.play();
        player.iterate();
        when(mockCurrentTimeProvider.getTime()).thenReturn(15 * 1000L);

        player.iterate();

        verify(mockBuildPlayerEventListener).onBuildThisNow(firstItem, 0);
    }

    @Test
    public void iterate_fiveSecondsAlertTimeOffsetAndAtSixSecondsBeforeFirstItem_listenerIsNotNotifiedToBuildFirstItem() throws Exception {
        BuildItem firstItem = new BuildItem(15, "probe");
        BuildItem secondItem = new BuildItem(140, "pylon");
        initPlayerWithItems(Arrays.asList(firstItem, secondItem));
        player.setAlertOffset(5);

        when(mockCurrentTimeProvider.getTime()).thenReturn(0L);
        player.play();
        player.iterate();
        when(mockCurrentTimeProvider.getTime()).thenReturn(9 * 1000L);

        player.iterate();

        verify(mockBuildPlayerEventListener, never()).onBuildThisNow(firstItem, 0);
    }

    private void initPlayerWithItems(List<BuildItem> items) {
        player = new BuildPlayer(mockCurrentTimeProvider, items);
        player.registerListener(mockBuildPlayerEventListener);
    }

}