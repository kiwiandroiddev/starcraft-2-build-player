package com.kiwiandroiddev.sc2buildassistant.feature.player.domain

import com.kiwiandroiddev.sc2buildassistant.domain.entity.BuildItem

import java.io.Serializable

/**
 * Encapsulates the low-level logic of playing back a build order. Objects
 * that implement the BuildPlayerEventListener interface can be attached
 * to be notified when playback events occur, such as when
 * an item should be built, or the playback was stopped/paused/etc.

 * @author matt
 */
class BuildPlayer(private val mCurrentTimeProvider: CurrentTimeProvider,
                  private val mItems: List<BuildItem>) : Serializable {

    companion object {
        private const val serialVersionUID = 5709010570800905185L
    }

    // FIXME: this isn't serializable - will throw IOException if non-null on serialize
    private var mListener: BuildPlayerEventListener? = null

    var isStopped = true
        private set

    var isPaused = false
        private set                             // implicitly: playing = (!mStopped && !mPaused)

    private var mCurrentGameTime = 0.0          // milliseconds of game time (not real time!)
    private var mTimeMultiplier = 1.0           // how to convert to game time from real time
    private var mBuildPointer = 0               // index of next item to build in mBuild's list
    private var mRefTime: Long = 0              // reference point in time used to calculate playback time elapsed (ms)
    private var mSeekOffsetMs: Long = 0

    /*
     * Sets how much early warning should be given to user for build item alerts
     * (can also be negative for delayed warning)
     */
    var alertOffsetInGameSeconds = 5                // early warning for build item alerts

    /*
     * Sets the time value (in game seconds) that playback should revert to when
     * player is stopped
     */
    var startTimeInGameSeconds = 0
        set(gameSeconds) {
            field = gameSeconds
            mStartTimeChanged = true
        }

    // FIXME: this isn't serializable - will throw IOException if non-null on serialize
    var buildItemFilter: ((BuildItem) -> Boolean)? = null
        get() = field
        set(predicate) {
            mFilterChanged = true
            field = predicate
        }

    // the following are used for state change tracking in iterate()
    private var mOldBuildPointer = 0
    private var mWasPaused = false
    private var mWasStopped = true
    private var mMultiplierChanged = false
    private var mStartTimeChanged = true    // fake change event to force initialization
    private var mFilterChanged = false

    private val filteredItems: List<BuildItem>
        get() = mItems.filter(buildItemFilter ?: { _ -> true })

    /*
     * Return duration of this build order in seconds
     * (i.e. returns the timestamp of last unit or building in build order)
     */
    val duration: Int
        get() = filteredItems.lastOrNull()?.time ?: 0       // NOTE: assumes build items are already sorted from first->last build time

    val isPlaying: Boolean
        get() = !isStopped && !isPaused

    fun setListener(newListener: BuildPlayerEventListener) {
        mListener = newListener

        newListener.initFromCurrentPlayerState()
    }

    private fun BuildPlayerEventListener.initFromCurrentPlayerState() {
        when {
            isPlaying -> onBuildPlay()
            isStopped -> onBuildStopped()
            isPaused -> onBuildPaused()
        }
    }

    fun clearListener() {
        mListener = null
    }

    /*
     * Advances playback of build, fires build events to listeners if appropriate.
     */
    fun iterate() {
        notifyFilterChangedIfNeeded()

        if (!isStopped && buildFinished()) {
            isStopped = true
            // fire onBuildFinished() event
            mListener?.onBuildFinished()
        }
        if (mMultiplierChanged) {
            // user changed game speed, need to set a new reference to prevent
            // new time multiplier from applying retroactively
            if (!this.isStopped) {
                mSeekOffsetMs = mCurrentGameTime.toLong()
                updateReferencePoint()
            }
            mMultiplierChanged = false
        }
        if (mStartTimeChanged && this.isStopped) {
            mSeekOffsetMs = (startTimeInGameSeconds * 1000).toLong()
            mStartTimeChanged = false
        }

        if (!mWasStopped && isStopped) {
            // playing -> stopped
            resetPlaybackState()

            // fire onStopped() event
            mListener?.onBuildStopped()
        } else if (mWasStopped && !isStopped) {
            // stopped -> playing
            updateReferencePoint()

            // fire onPlay() event
            mListener?.onBuildPlay()
        } else if (!mWasPaused && isPaused) {
            // playing -> paused
            mSeekOffsetMs = mCurrentGameTime.toLong()

            // fire onPaused() event
            mListener?.onBuildPaused()

        } else if (mWasPaused && !isPaused) {
            // paused -> playing
            updateReferencePoint()

            // fire onResume() event
            mListener?.onBuildResumed()
        }

        mWasStopped = isStopped
        mWasPaused = isPaused

        val gameTimeToShowListeners: Long
        if (this.isPlaying) {
            val currentTime = mCurrentTimeProvider.time
            mCurrentGameTime = (currentTime - mRefTime) * mTimeMultiplier + mSeekOffsetMs
            gameTimeToShowListeners = mCurrentGameTime.toLong()
            doBuildAlerts()    // see if user should be told to build something
        } else {
            // bit hacky
            gameTimeToShowListeners = mSeekOffsetMs
        }

        mListener?.onIterate(gameTimeToShowListeners)
    }

    private fun notifyFilterChangedIfNeeded() {
        if (mFilterChanged) {
            mListener?.onBuildItemsChanged(filteredItems)
            mFilterChanged = false
        }
    }

    private fun resetPlaybackState() {
        mCurrentGameTime = 0.0
        mSeekOffsetMs = (startTimeInGameSeconds * 1000).toLong()
        mBuildPointer = 0
        isPaused = false    // reset paused state if true
        mRefTime = 0
        mOldBuildPointer = 0
        mStartTimeChanged = true    // fake change event to force initialization
    }

    fun setTimeMultiplier(factor: Double) {
        if (factor != mTimeMultiplier) {
            mTimeMultiplier = factor
            mMultiplierChanged = true
        }
    }

    fun seekTo(gameTimeMs: Long) {
        mSeekOffsetMs = gameTimeMs
        updateReferencePoint()
    }

    fun play() {
        isStopped = false
        isPaused = false
    }

    fun pause() {
        if (!isStopped)
            isPaused = true
    }

    fun stop() {
        isStopped = true
    }

    /*
     * The build being finished is defined as the build pointer pointing past the
     * end of the build item array
     */
    fun buildFinished(): Boolean {
        if (mBuildPointer >= mItems.size || mItems.isEmpty() || filteredItems.isEmpty())
            return true

        return noMoreFilteredBuildItemsRemaining()
    }

    private fun noMoreFilteredBuildItemsRemaining(): Boolean {
        val currentUnfilteredItem = mItems[mBuildPointer]
        val lastFilteredItem = filteredItems.last()
        return currentUnfilteredItem.time > lastFilteredItem.time
    }

    private fun updateReferencePoint() {
        mRefTime = mCurrentTimeProvider.time
    }

    /*
     * checks if the user should be building/training any new units
     * since the last update (call to iterate()), and if so, tells the
     * user to build them.
     */
    private fun doBuildAlerts() {
        // update build item pointer (possibly backwards)
        findNewNextUnit()

        // did the user seek back in time?
        if (mOldBuildPointer > mBuildPointer) {
            mOldBuildPointer = mBuildPointer
        }

        // If the build pointer has increased in value since last time,
        // we need to tell the user to build one or more units
        // If the user has seeked back in time, this loop has no effect
        while (mOldBuildPointer < mBuildPointer) {
            // TODO: we don't really want to call this if the user has seeked far ahead
            // (to prevent a barrage of build orders)
            val item = mItems[mOldBuildPointer]
            val itemPassesFilter = buildItemFilter?.invoke(item) ?: true
            if (itemPassesFilter) {
                val filteredItemPosition = filteredItems.indexOf(item)
                mListener?.onBuildThisNow(item, filteredItemPosition)
            }

            mOldBuildPointer++
        }
    }

    /*
     * Moves the build item pointer with respect to current playback time.
     * Changes in item pointer position are picked up elsewhere (iterate())
     * and translated into alerts for the user.
     */
    private fun findNewNextUnit() {
        // reposition "next unit" pointer based on current game time
        // this gets slightly tricky because user can seek back in time

        // if the current game time has decreased since this was last called,
        // (passing one or more unit build times), decrease the pointer until we find
        // the correct new position

        // if current game time is less than the build time of a unit we've already
        // told the user to build...
        if (mBuildPointer > 0) {
            var previousItem = mItems[mBuildPointer - 1]

            // convert from seconds (JSON build file) to ms (used internally)
            while (mCurrentGameTime <= (previousItem.time - alertOffsetInGameSeconds) * 1000) {
                mBuildPointer--
                if (mBuildPointer == 0)
                    return

                previousItem = mItems[mBuildPointer - 1]
            }
        }

        if (buildFinished()) {
            return
        }

        // if the current game time has increased since this was last called,
        // (passing one or more unit build times), increase the pointer until we find
        // the correct new position
        var nextItem = mItems[mBuildPointer]

        // convert from seconds (JSON build file) to ms (used internally)
        while (mCurrentGameTime >= (nextItem.time - alertOffsetInGameSeconds) * 1000) {
            mBuildPointer++
            if (buildFinished())
                return

            nextItem = mItems[mBuildPointer]
        }
    }

    fun clearBuildItemFilter() {
        mFilterChanged = true
        buildItemFilter = null
    }

}
