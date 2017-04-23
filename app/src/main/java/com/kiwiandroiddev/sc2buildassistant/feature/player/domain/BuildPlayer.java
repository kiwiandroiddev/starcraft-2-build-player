package com.kiwiandroiddev.sc2buildassistant.feature.player.domain;

import android.support.annotation.NonNull;

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the low-level logic of playing back a build order. Objects
 * that implement the BuildPlayerEventListener interface can be attached
 * to be notified when playback events occur, such as when
 * an item should be built, or the playback was stopped/paused/etc.
 * 
 * @author matt
 *
 */
public class BuildPlayer implements Serializable {
	
	private static final long serialVersionUID = 5709010570800905185L;
	
	private List<BuildPlayerEventListener> mListeners = new ArrayList<>();
	private CurrentTimeProvider mCurrentTimeProvider;
	private List<BuildItem> mItems;
	private boolean mStopped = true;
	private boolean mPaused = false;			// implicitly: playing = (!mStopped && !mPaused)
	private double mCurrentGameTime = 0f;		// milliseconds of game time (not real time!)
	private double mTimeMultiplier = 1.0f;		// how to convert to game time from real time
	private int mBuildPointer = 0;				// index of next item to build in mBuild's list
	private long mRefTime = 0;					// reference point in time used to calculate playback time elapsed (ms)
	private long mSeekOffset;				// (ms)
	private int mAlertOffset = 5;				// early warning for build item alerts, in game seconds
	private int mStartTime = 0;					// what progress to set build to when stop is pressed (game seconds)
	
	// the following are used for state change tracking in iterate()
	private int mOldBuildPointer = 0;
	private boolean mWasPaused = false;
	private boolean mWasStopped = true;
	private boolean mMultiplierChanged = false;
	private boolean mStartTimeChanged = true;	// fake change event to force initialization
	
	//=========================================================================
	// Public methods
	//=========================================================================
	
	public BuildPlayer(@NonNull CurrentTimeProvider currentTimeProvider, @NonNull List<BuildItem> items) {
		mCurrentTimeProvider = currentTimeProvider;
		mItems = items;
	}
	
	// ------------------------------------------------------------------------
	// Accessors
	// ------------------------------------------------------------------------
	
	/*
	 * return duration of this build order in seconds
	 * (i.e. returns the timestamp of last unit or building in build order)
	 */
	public int getDuration() {
		if (mItems.size() == 0) {
			return 0;	// silently fail
		}
		// NOTE: assumes build items are already sorted from first->last build time
		return mItems.get(mItems.size()-1).getTime();
	}
	
	public boolean isPlaying() {
		return (!mStopped && !mPaused);
	}
	
	public boolean isStopped() {
		return mStopped;
	}
	
	public boolean isPaused() {
		return mPaused;
	}

	/*
	 * returns how much early warning is given to user of build items, in game seconds
	 */
	public int getAlertOffset() {
		return mAlertOffset;
	}
	
	/*
	 * returns time value (in game seconds) that playback is set to when player is stopped
	 */
	public int getStartTime() {
		return mStartTime;
	}

	// ------------------------------------------------------------------------
	// Mutators
	// ------------------------------------------------------------------------	

	public void registerListener(BuildPlayerEventListener newListener) {
		mListeners.add(newListener);

		// initialize the new listener's state (make this optional?)
		if (this.isPlaying())
			newListener.onBuildPlay();
		else if (this.isStopped())
			newListener.onBuildStopped();
		else if (this.isPaused())
			newListener.onBuildPaused();
	}
	
	public void removeListeners() {
		mListeners.clear();
	}
	
	/*
	 * Advances playback of build, fires build events to listeners if appropriate.
	 */
	public void iterate() {
		if (!mStopped && buildFinished()) {
			mStopped = true;
			// fire onBuildFinished() event
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildFinished();
		}
		if (mMultiplierChanged) {
			// user changed game speed, need to set a new reference to prevent
			// new time multiplier from applying retroactively
			if (!this.isStopped()) {
				mSeekOffset = (long)mCurrentGameTime;
				updateReferencePoint();
			}
			mMultiplierChanged = false;
		}
		if (mStartTimeChanged && this.isStopped()) {
			mSeekOffset = (mStartTime * 1000);
			mStartTimeChanged = false;
		}
		
		if (!mWasStopped && mStopped) {
			// playing -> stopped
			resetPlaybackState();
			
			// fire onStopped() event
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildStopped();
		} else if (mWasStopped && !mStopped) {
			// stopped -> playing
			updateReferencePoint();

			// fire onPlay() event
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildPlay();
		} else if (!mWasPaused && mPaused) {
			// playing -> paused
			mSeekOffset = (long)mCurrentGameTime;

			// fire onPaused() event
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildPaused();
			
		} else if (mWasPaused && !mPaused) {
			// paused -> playing
			updateReferencePoint();
			
			// fire onResume() event
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildResumed();
		}

		mWasStopped = mStopped;
		mWasPaused = mPaused;
		
		long gameTimeToShowListeners;
		if (this.isPlaying()) {
			long currentTime = mCurrentTimeProvider.getTime();
			mCurrentGameTime = (currentTime - mRefTime) * mTimeMultiplier + mSeekOffset;
			gameTimeToShowListeners = (long)mCurrentGameTime;
			doBuildAlerts();	// see if user should be told to build something
		} else {
			// bit hacky
			gameTimeToShowListeners = mSeekOffset;
		}
		
//		Log.w(this.toString(), "game time to user = " + gameTimeToShowListeners);
		for (BuildPlayerEventListener listener : mListeners)
			listener.onIterate(gameTimeToShowListeners);
	}

	private void resetPlaybackState() {
		mCurrentGameTime = 0;
		mSeekOffset = (mStartTime * 1000);
		mBuildPointer = 0;
		mPaused = false;	// reset paused state if true
		mRefTime = 0;
		mOldBuildPointer = 0;
		mStartTimeChanged = true;	// fake change event to force initialization
	}

	public void setTimeMultiplier(double factor) {
		if (factor != mTimeMultiplier) {
			mTimeMultiplier = factor;
			mMultiplierChanged = true;
		}
	}
	
	// gameTime - milliseconds
	public void seekTo(long gameTime) {
		mSeekOffset = gameTime;
		updateReferencePoint();
	}
	
	public void play() {
		mStopped = false;
		mPaused = false;
	}
	
	public void pause() {
		if (mStopped == false)
			mPaused = true;
	}
	
	public void stop() {
		mStopped = true;
	}
	
	/*
	 * Sets how much early warning should be given to user for build item alerts
	 * (can also be negative for delayed warning)
	 */
	public void setAlertOffset(int gameSeconds) {
		mAlertOffset = gameSeconds;
	}
	
	/*
	 * Sets the time value (in game seconds) that playback should revert to when
	 * player is stopped
	 */
	public void setStartTime(int gameSeconds) {
		mStartTime = gameSeconds;
		mStartTimeChanged = true;
	}
	
	/*
	 * the build being finished is defined as the build pointer pointing past the
	 * end of the build item array
	 */
	public boolean buildFinished() {
		return (mBuildPointer >= mItems.size()) ||
			   (mItems.size() == 0);		// should probably raise an error in this condition
	}
	
	//=========================================================================
	// Private methods
	//=========================================================================

	private void updateReferencePoint() {
		mRefTime = mCurrentTimeProvider.getTime();
	}
	
	/*
	 * Moves the build item pointer with respect to current playback time.
	 * Changes in item pointer position are picked up elsewhere (iterate())
	 * and translated into alerts for the user.
	 */
	private void findNewNextUnit() {
		// reposition "next unit" pointer based on current game time
		// this gets slightly tricky because user can seek back in time

		// if the current game time has decreased since this was last called,
		// (passing one or more unit build times), decrease the pointer until we find
		// the correct new position
		
		// if current game time is less than the build time of a unit we've already
		// told the user to build...
		if (mBuildPointer > 0) {
			BuildItem previousItem = mItems.get(mBuildPointer-1);

			// convert from seconds (JSON build file) to ms (used internally)
			while (mCurrentGameTime <= ((previousItem.getTime() - getAlertOffset()) * 1000)) {
//				Log.w("test", "game time has decreased, decrement pointer...");	// stub
				mBuildPointer--;
				if (mBuildPointer == 0)
					return;

				previousItem = mItems.get(mBuildPointer-1);
			}
		}
		
		if (buildFinished()) {
//			Log.w("test", "build finished, don't try to advance pointer");
			return;
		}
		
		// if the current game time has increased since this was last called,
		// (passing one or more unit build times), increase the pointer until we find
		// the correct new position
		BuildItem nextItem = mItems.get(mBuildPointer);

		// convert from seconds (JSON build file) to ms (used internally)
		while (mCurrentGameTime >= ((nextItem.getTime() - getAlertOffset()) * 1000)) {
			mBuildPointer++;
			if (buildFinished())
				return;

			nextItem = mItems.get(mBuildPointer);
		}
	}
	
	/*
	 * checks if the user should be building/training any new units
	 * since the last update (call to iterate()), and if so, tells the
	 * user to build them.
	 */
	private void doBuildAlerts() {
		// update build item pointer (possibly backwards)
		findNewNextUnit();
		
		// has the user seeked back in time?
		if (mOldBuildPointer > mBuildPointer) {
			mOldBuildPointer = mBuildPointer;
		}
		
		// If the build pointer has increased in value since last time,
		// we need to tell the user to build one or more units
		// If the user has seeked back in time, this loop has no effect
		while (mOldBuildPointer < mBuildPointer) {
			// TODO: we don't really want to call this if the user has seeked far ahead
			// (to prevent a barrage of build orders)
			BuildItem item = mItems.get(mOldBuildPointer);
			for (BuildPlayerEventListener listener : mListeners)
				listener.onBuildThisNow(item, mOldBuildPointer);

			mOldBuildPointer++;
		}
	}

	@Override
	public String toString() {
		return "BuildPlayer{" +
				"mCurrentTimeProvider=" + mCurrentTimeProvider +
				", mItems=" + mItems +
				", mStopped=" + mStopped +
				", mPaused=" + mPaused +
				", mCurrentGameTime=" + mCurrentGameTime +
				", mTimeMultiplier=" + mTimeMultiplier +
				", mBuildPointer=" + mBuildPointer +
				", mRefTime=" + mRefTime +
				", mSeekOffset=" + mSeekOffset +
				", mAlertOffset=" + mAlertOffset +
				", mStartTime=" + mStartTime +
				", mOldBuildPointer=" + mOldBuildPointer +
				", mWasPaused=" + mWasPaused +
				", mWasStopped=" + mWasStopped +
				", mMultiplierChanged=" + mMultiplierChanged +
				", mStartTimeChanged=" + mStartTimeChanged +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BuildPlayer that = (BuildPlayer) o;

		if (mStopped != that.mStopped) return false;
		if (mPaused != that.mPaused) return false;
		if (Double.compare(that.mCurrentGameTime, mCurrentGameTime) != 0) return false;
		if (Double.compare(that.mTimeMultiplier, mTimeMultiplier) != 0) return false;
		if (mBuildPointer != that.mBuildPointer) return false;
		if (mRefTime != that.mRefTime) return false;
		if (mSeekOffset != that.mSeekOffset) return false;
		if (mAlertOffset != that.mAlertOffset) return false;
		if (mStartTime != that.mStartTime) return false;
		if (mOldBuildPointer != that.mOldBuildPointer) return false;
		if (mWasPaused != that.mWasPaused) return false;
		if (mWasStopped != that.mWasStopped) return false;
		if (mMultiplierChanged != that.mMultiplierChanged) return false;
		if (mStartTimeChanged != that.mStartTimeChanged) return false;
		if (mListeners != null ? !mListeners.equals(that.mListeners) : that.mListeners != null)
			return false;
		if (!mCurrentTimeProvider.equals(that.mCurrentTimeProvider)) return false;
		return mItems.equals(that.mItems);

	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = mListeners != null ? mListeners.hashCode() : 0;
		result = 31 * result + mCurrentTimeProvider.hashCode();
		result = 31 * result + mItems.hashCode();
		result = 31 * result + (mStopped ? 1 : 0);
		result = 31 * result + (mPaused ? 1 : 0);
		temp = Double.doubleToLongBits(mCurrentGameTime);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mTimeMultiplier);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + mBuildPointer;
		result = 31 * result + (int) (mRefTime ^ (mRefTime >>> 32));
		result = 31 * result + (int) (mSeekOffset ^ (mSeekOffset >>> 32));
		result = 31 * result + mAlertOffset;
		result = 31 * result + mStartTime;
		result = 31 * result + mOldBuildPointer;
		result = 31 * result + (mWasPaused ? 1 : 0);
		result = 31 * result + (mWasStopped ? 1 : 0);
		result = 31 * result + (mMultiplierChanged ? 1 : 0);
		result = 31 * result + (mStartTimeChanged ? 1 : 0);
		return result;
	}
}
