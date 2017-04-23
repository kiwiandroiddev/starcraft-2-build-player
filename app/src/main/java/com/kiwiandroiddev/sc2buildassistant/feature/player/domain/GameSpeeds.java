package com.kiwiandroiddev.sc2buildassistant.feature.player.domain;

import android.support.annotation.NonNull;

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Expansion;

/**
 * Created by matt on 18/04/16.
 */
public final class GameSpeeds {

    // StarCraft 2 game speed reference: http://wiki.teamliquid.net/starcraft2/Game_Speed
    private static final double SLOWER_FACTOR = 0.6;
    private static final double SLOW_FACTOR = 0.8;
    private static final double NORMAL_FACTOR = 1.0;
    private static final double FAST_FACTOR = 1.2;
    private static final double FASTER_FACTOR = 1.4;

    private static double[] sWolGameSpeedIndexToMultiplier =
            { SLOWER_FACTOR, SLOW_FACTOR, NORMAL_FACTOR, FAST_FACTOR, FASTER_FACTOR };

    private static final double LOTV_SLOWER_FACTOR = 0.43405;
    private static final double LOTV_SLOW_FACTOR = 0.60144;
    private static final double LOTV_NORMAL_FACTOR = 0.72463;
    private static final double LOTV_FAST_FACTOR = 0.87681;
    private static final double LOTV_FASTER_FACTOR = 1.0;
    
    private static double[] sLotvGameSpeedIndexToMultiplier =
            { LOTV_SLOWER_FACTOR, LOTV_SLOW_FACTOR, LOTV_NORMAL_FACTOR, LOTV_FAST_FACTOR, LOTV_FASTER_FACTOR };

    private GameSpeeds() {}

    /**
     * Returns the real-time -> game-time multiplier for a given game speed and SC2 expansion
     * where valid game speed indices are in the range of 0 to 4 (corresponding to slower up to faster)
     */
    public static double getMultiplierForGameSpeed(int gameSpeedIndex, @NonNull Expansion expansion) {
        if (gameSpeedIndex < 0 || gameSpeedIndex >= sWolGameSpeedIndexToMultiplier.length) {
            throw new IllegalArgumentException("Invalid game speed index: " + gameSpeedIndex);
        }

        if (expansion == Expansion.WOL || expansion == Expansion.HOTS) {
            return sWolGameSpeedIndexToMultiplier[gameSpeedIndex];
        } else if (expansion == Expansion.LOTV) {
            return sLotvGameSpeedIndexToMultiplier[gameSpeedIndex];
        }

        throw new IllegalArgumentException("Invalid expansion: " + expansion);
    }
    
}
