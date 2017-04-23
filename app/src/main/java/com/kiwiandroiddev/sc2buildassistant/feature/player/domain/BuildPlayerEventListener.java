package com.kiwiandroiddev.sc2buildassistant.feature.player.domain;


import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;

/** Interface used by BuildPlayer to notify high-level code of playback events */
public interface BuildPlayerEventListener {
	void onBuildThisNow(BuildItem item, int position);
	void onBuildPlay();
	void onBuildPaused();
	void onBuildStopped();
	void onBuildResumed();
	void onBuildFinished();
	void onIterate(long newGameTimeMs);
}
