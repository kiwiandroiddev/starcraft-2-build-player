package com.kiwiandroiddev.sc2buildassistant.domain.entity;

import java.io.Serializable;


/**
 * Represents some action in a build order, whether it be to build a unit or structure, research
 * an upgrade or use some ability. Has the time in game seconds when the action should occur.
 */
public class BuildItem implements Serializable {
	private static final long serialVersionUID = -2498800960454128091L;
	private int mCount = 1;	// number of this unit to build/train
	private int mTime = 0;	// game time in seconds when this unit/structure should be built (2:00 = 120)
	private String mUnit;
	private String mTarget;		// for abilities (and terran addon buildings), defines target unit
	private String mText;	// if specified, will show to user in visual alert
	private String mVoice;	// if specified, will be spoken to user using TTS engine
	
	public BuildItem() {
	}
	
	public BuildItem(int timeInSeconds, String unit) {
		mTime = timeInSeconds;
		mUnit = unit;
		mCount = 1;
	}
	
	public BuildItem(int timeInSeconds, String unit, int count) {
		mTime = timeInSeconds;
		mUnit = unit;
		mCount = count;
	}
	
	public BuildItem(int timeInSeconds, String unit, int count, String target, String text, String voice) {
		mTime = timeInSeconds;
		mUnit = unit;
		mCount = count;
		mTarget = target;
		mText = text;
		mVoice = voice;
	}
	
	@Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        BuildItem rhs = (BuildItem) obj;
        return mTime == rhs.getTime() &&
        		mCount == rhs.getCount() &&
        		Build.Companion.objectsEquivalent(mUnit, rhs.getGameItemID()) &&
        		Build.Companion.objectsEquivalent(mTarget, rhs.getTarget()) &&
        		Build.Companion.objectsEquivalent(mText, rhs.getText()) &&
        		Build.Companion.objectsEquivalent(mVoice, rhs.getVoice());
    }
	
	public String toString() {
		return mUnit + " x" + mCount + " @ " + mTime + "s, target=" + mTarget + ", text=" + mText + ", voice=" + mVoice;
	}
	
	public int getCount() {
		return mCount;
	}
	
	public int getTime() {
		return mTime;
	}
	
	public String getGameItemID() {
		return mUnit;
	}
	
	/*
	 * For abilities (and terran addon buildings), defines target unit.
	 * String should be a valid game item ID.
	 */
	public String getTarget() {
		return mTarget;
	}
	
	/*
	 * Return the text message that should be shown to the user in a visual
	 * alert. If null or empty, a message should be generated from unit and
	 * count fields.
	 */
	public String getText() {
		return mText;
	}
	
	/*
	 * Return the text message that should be spoken to the user using the TTS
	 * engine for this build item. If null or empty, a message should be
	 * generated from unit and count fields.
	 */
	public String getVoice() {
		return mVoice;
	}
	
	public void setCount(int count) {
		mCount = count;
	}
	
	public void setText(String text) {
		mText = text;
	}
	
	public void setVoice(String voice) {
		mVoice = voice;
	}
	
	public void setTarget(String target) {
		mTarget = target;
	}
	
	public void setTime(int seconds) {
		mTime = seconds;
	}
	
	public void setUnit(String unit) {
		mUnit = unit;
	}
	
	public long longHashCode() {
		long hash = 1;
        hash = hash * 41 + mCount;
        hash = hash * 11 + mTime;
        hash = hash * 15 + mUnit.hashCode();
        hash = hash * 13 + (mTarget == null ? 0 : mTarget.hashCode());
        hash = hash * 17 + (mText == null ? 0 : mText.hashCode());
        hash = hash * 51 + (mVoice == null ? 0 : mVoice.hashCode());
        return hash;
	}
}
