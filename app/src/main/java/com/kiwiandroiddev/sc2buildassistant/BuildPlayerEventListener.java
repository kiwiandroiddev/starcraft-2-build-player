package com.kiwiandroiddev.sc2buildassistant;


/** Interface used by BuildPlayer to notify high-level code of playback events */
public interface BuildPlayerEventListener {
	public void onBuildThisNow(BuildItem item, int position);
	public void onBuildPlay();
	public void onBuildPaused();
	public void onBuildStopped();
	public void onBuildResumed();
	public void onBuildFinished();
	public void onIterate(long newGameTime);	// game time in milliseconds
//	public void onProgressChanged(long newGameTime);
}
