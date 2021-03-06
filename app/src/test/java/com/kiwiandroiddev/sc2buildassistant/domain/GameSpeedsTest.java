package com.kiwiandroiddev.sc2buildassistant.domain;

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;
import com.kiwiandroiddev.sc2buildassistant.feature.player.domain.GameSpeeds;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by matt on 18/04/16.
 */
public class GameSpeedsTest {

    private static final int SLOWER_INDEX = 0;
    private static final int SLOW_INDEX = 1;
    private static final int NORMAL_INDEX = 2;
    private static final int FAST_INDEX = 3;
    private static final int FASTER_INDEX = 4;

    @org.junit.Test
    public void testGetMultiplierForGameSpeed_validIndices() {
        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOWER_INDEX, Expansion.WOL)).isEqualTo(0.6d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOW_INDEX, Expansion.WOL)).isEqualTo(0.8d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(NORMAL_INDEX, Expansion.WOL)).isEqualTo(1.0d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FAST_INDEX, Expansion.WOL)).isEqualTo(1.2d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FASTER_INDEX, Expansion.WOL)).isEqualTo(1.4d);

        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOWER_INDEX, Expansion.HOTS)).isEqualTo(0.6d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOW_INDEX, Expansion.HOTS)).isEqualTo(0.8d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(NORMAL_INDEX, Expansion.HOTS)).isEqualTo(1.0d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FAST_INDEX, Expansion.HOTS)).isEqualTo(1.2d);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FASTER_INDEX, Expansion.HOTS)).isEqualTo(1.4d);

        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOWER_INDEX, Expansion.LOTV)).isEqualTo(0.43405);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(SLOW_INDEX, Expansion.LOTV)).isEqualTo(0.60144);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(NORMAL_INDEX, Expansion.LOTV)).isEqualTo(0.72463);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FAST_INDEX, Expansion.LOTV)).isEqualTo(0.87681);
        assertThat(GameSpeeds.getMultiplierForGameSpeed(FASTER_INDEX, Expansion.LOTV)).isEqualTo(1.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMultiplierForGameSpeed_negativeGameSpeedIndex() {
        GameSpeeds.getMultiplierForGameSpeed(-1, Expansion.WOL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetMultiplierForGameSpeed_gameSpeedIndexTooLarge() {
        GameSpeeds.getMultiplierForGameSpeed(5, Expansion.WOL);
    }
}