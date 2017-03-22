package com.kiwiandroiddev.sc2buildassistant.domain;

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by matt on 23/03/17.
 */
public class BuildPlayerTest {

    @Test
    public void playThenStop_buildPlayerStateShouldBeTheSameAsBeforePlaybackStarted() throws Exception {
        ArrayList<BuildItem> items = new ArrayList<>(Arrays.asList(
                new BuildItem(120, "pylon"),
                new BuildItem(150, "gateway")
        ));
        BuildPlayer player = new BuildPlayer(items);
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
        BuildPlayer player = new BuildPlayer(items);

        int duration = player.getDuration();

        assertThat(duration).isEqualTo(150);
    }

    @Test
    public void getDuration_noBuildItems_durationIsZero() throws Exception {
        BuildPlayer player = new BuildPlayer(new ArrayList<BuildItem>());

        int duration = player.getDuration();

        assertThat(duration).isEqualTo(0);
    }

}