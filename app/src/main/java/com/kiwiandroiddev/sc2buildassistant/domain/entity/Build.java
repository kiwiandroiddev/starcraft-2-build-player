package com.kiwiandroiddev.sc2buildassistant.domain.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Encapsulates all information about a particular build order
 * including its name, the race it's for, and the units with timestamps 
 */
public class Build implements Serializable {

	private static final long serialVersionUID = -3970314516973612524L;

	private Date mCreated;		// time this build was first created
	private Date mModified;		// time this build was last modified
	private String mName;
	private String mNotes;
	private String mSource;		// original forum post, web page, etc. Can contain HTML (e.g. hyperlinks)
	private String mAuthor;		// author, person who transcribed the build
	private ArrayList<BuildItem> mItems;	// buildings/units in this build order. Important: assumed to be ordered from first->last!
	private Faction mFaction;
	private Faction mVsFaction;
	private Expansion mExpansion;
	
	public Build() {
		super();
		mExpansion = Expansion.WOL;
	}
	
	public Build(String name, Faction race) {
		super();
		mName = name;
		mFaction = race;
		mItems = null;
		mExpansion = Expansion.WOL;
	}
	
	public Build(String name, Faction race, ArrayList<BuildItem> items) {
		super();
		mName = name;
		mFaction = race;
		mItems = items;
		mExpansion = Expansion.WOL;
	}
	
	public Build(String name, Faction race, Faction vsRace,
				 Expansion expansion, String source, String notes, ArrayList<BuildItem> items) {
		super();
		mName = name;
		mFaction = race;
		mVsFaction = vsRace;
		mItems = items;
		mSource = source;
		mNotes = notes;
		mExpansion = expansion;
	}
		
	@Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        Build rhs = (Build) obj;
        boolean result = mFaction == rhs.getFaction() &&
        		mVsFaction == rhs.getVsFaction() &&
        		mExpansion == rhs.getExpansion() &&
        		objectsEquivalent(mName, rhs.getName()) &&
        		objectsEquivalent(mSource, rhs.getSource()) &&
        		objectsEquivalent(mAuthor, rhs.getAuthor()) &&
        		objectsEquivalent(mNotes, rhs.getNotes()) &&
        		objectsEquivalent(mItems, rhs.getItems());
        return result;
    }
	
	public static boolean objectsEquivalent(Object lhs, Object rhs) {
		return (lhs == rhs || (lhs != null && lhs.equals(rhs)));
	}
	
	// accessors
	public String getName() {
		return mName;
	}
	
	public String toString() {
		// stub
		return mName;
	}
	
	public ArrayList<BuildItem> getItems() {
		return mItems;
	}
	
	public Faction getFaction() {
		return mFaction;
	}
	
	public Faction getVsFaction() {
		return mVsFaction;
	}
		
	public String getNotes() {
		return mNotes;
	}
	
	/* returns author or original forum post, etc. Can contain HTML (e.g. hyperlinks) */
	public String getSource() {
		return mSource;
	}
	
	/* return the transcriber's name (can be null) */
	public String getAuthor() {
		return mAuthor;
	}
	
	/**
	 * Assuming the source string is an html link (<a> tag), returns the plain text component
	 * If it's not an <a> tag, return the full source string
	 * @return
	 */
	public String getSourceTitle() {
		if (mSource == null)
			return null;
		
		Pattern pattern = Pattern.compile(">(.*?)<");
		Matcher matcher = pattern.matcher(mSource);
		if (matcher.find())	{
		    return matcher.group(1);
		} else {
			return mSource;
		}
	}
	
	/**
	 * Assuming the source string is an html link (<a> tag), returns the URL component
	 * If it's not an <a> tag, return the full source string
	 * @return
	 */
	public String getSourceURL() {
		if (mSource == null)
			return null;
		
		Pattern pattern = Pattern.compile("\"(.*?)\"");
		Matcher matcher = pattern.matcher(mSource);
		if (matcher.find())	{
		    return matcher.group(1);
		} else {
			return mSource;
		}
	}
	
	public Expansion getExpansion() {
		return mExpansion;
	}

	/*
	 * return duration of this build order in seconds
	 * (i.e. returns the timestamp of last unit or building in build order)
	 */
	public long getDuration() {
		if (mItems.size() == 0) {
			return 0;	// silently fail
		}
		// assumes build items are already sorted from first->last build time!
		return mItems.get(mItems.size()-1).getTime();
	}
	
	public Date getCreated() {
		return mCreated;
	}
	
	public Date getModified() {
		return mModified;
	}
	
	// TODO: take into account null fields
	public long longHashCode() {
        long hash = 1;
        hash = hash * 5 + mName.hashCode();
        hash = hash * 31 + mNotes.hashCode();
        hash = hash * 11 + mSource.hashCode();
        hash = hash * 5 + mSource.hashCode();
        hash = hash * 17 + mAuthor.hashCode();
        hash = hash * 3 + mFaction.ordinal();
        hash = hash * 19 + mExpansion.ordinal();
        hash = hash * 13 + (mVsFaction == null ? 0 : mVsFaction.ordinal());
        if (mItems != null) {
        	for (BuildItem item : mItems) {
        		hash = hash * 3 + item.longHashCode();
        	}
        }
        return hash;
	}
	
	// Mutators
	
	public void setExpansion(Expansion expansion) {
		mExpansion = expansion;
	}
	
	public void setName(String name) {
		mName = name;
	}
	
	public void setFaction(Faction faction) {
		mFaction = faction;
	}
	
	public void setVsFaction(Faction vsFaction) {
		mVsFaction = vsFaction;
	}
	
	public void setSource(String source) {
		mSource = source;
	}
	
	public void setSource(String title, String url) {
		if (title == null) {
			if (url == null) {
				mSource = null;
			} else {
				mSource = url;
			}
		} else {
			if (url == null) {
				mSource = title;
			} else {
				mSource = "<a href=\"" + url + "\">" + title + "</a>";
			}
		}
	}
	
	public void setNotes(String notes) {
		mNotes = notes;
	}
	
	public void setAuthor(String author) {
		mAuthor = author;
	}
	
	public void setItems(ArrayList<BuildItem> items) {
		mItems = items;
	}
	
	public void setCreated(Date date) {
		mCreated = date;
	}
	
	public void setModified(Date date) {
		mModified = date;
	}
	
	/**
	 * @return true if this Build's items are ordered chronologically (as they should be)
	 */
	public boolean isWellOrdered() {
		return isWellOrdered(mItems);
	}
	
	/**
	 * Checks if a list of build items are in chronological order
	 * @param items
	 * @return
	 */
	public static boolean isWellOrdered(ArrayList<BuildItem> items) {
		if (items == null || items.size() <= 1)
			return true;
		
		for (int i=1; i<items.size(); i++) {
			if (items.get(i).getTime() < items.get(i-1).getTime())
				return false;
		}
		return true;
	}
}
