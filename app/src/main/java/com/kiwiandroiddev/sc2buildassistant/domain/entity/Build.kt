package com.kiwiandroiddev.sc2buildassistant.domain.entity

import java.io.Serializable
import java.util.ArrayList
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Encapsulates all information about a particular build order
 * including its name, the race it's for, and the units with timestamps
 */
class Build : Serializable {

    var created: Date? = null        // time this build was first created
    var modified: Date? = null        // time this build was last modified
    // accessors
    var name: String? = null
    var notes: String? = null
    /* returns author or original forum post, etc. Can contain HTML (e.g. hyperlinks) */
    var source: String? = null        // original forum post, web page, etc. Can contain HTML (e.g. hyperlinks)
    /* return the transcriber's name (can be null) */
    var author: String? = null        // author, person who transcribed the build
    var items: ArrayList<BuildItem>? = null    // buildings/units in this build order. Important: assumed to be ordered from first->last!
    var faction: Faction? = null
    var vsFaction: Faction? = null
    // Mutators

    var expansion: Expansion? = null

    constructor() : super() {
        expansion = Expansion.WOL
    }

    constructor(name: String, race: Faction) : super() {
        this.name = name
        faction = race
        items = null
        expansion = Expansion.WOL
    }

    constructor(name: String, race: Faction, items: ArrayList<BuildItem>) : super() {
        this.name = name
        faction = race
        this.items = items
        expansion = Expansion.WOL
    }

    constructor(name: String, race: Faction, vsRace: Faction,
                expansion: Expansion, source: String, notes: String, items: ArrayList<BuildItem>) : super() {
        this.name = name
        faction = race
        vsFaction = vsRace
        this.items = items
        this.source = source
        this.notes = notes
        this.expansion = expansion
    }

    override fun equals(obj: Any?): Boolean {
        if (obj === this) {
            return true
        }
        if (obj == null || obj.javaClass != this.javaClass) {
            return false
        }

        val rhs = obj as Build?
        val result = faction == rhs!!.faction &&
                vsFaction == rhs.vsFaction &&
                expansion == rhs.expansion &&
                objectsEquivalent(name, rhs.name) &&
                objectsEquivalent(source, rhs.source) &&
                objectsEquivalent(author, rhs.author) &&
                objectsEquivalent(notes, rhs.notes) &&
                objectsEquivalent(items, rhs.items)
        return result
    }

    override fun toString(): String {
        return "Build{" +
                "mCreated=" + created +
                ", mModified=" + modified +
                ", mName='" + name + '\'' +
                ", mNotes='" + notes + '\'' +
                ", mSource='" + source + '\'' +
                ", mAuthor='" + author + '\'' +
                ", mItems=" + items +
                ", mFaction=" + faction +
                ", mVsFaction=" + vsFaction +
                ", mExpansion=" + expansion +
                '}'
    }

    /**
     * Assuming the source string is an html link (<a> tag), returns the plain text component
     * If it's not an <a> tag, return the full source string
     * @return
    </a></a> */
    val sourceTitle: String?
        get() {
            if (source == null)
                return null

            val pattern = Pattern.compile(">(.*?)<")
            val matcher = pattern.matcher(source!!)
            if (matcher.find()) {
                return matcher.group(1)
            } else {
                return source
            }
        }

    /**
     * Assuming the source string is an html link (<a> tag), returns the URL component
     * If it's not an <a> tag, return the full source string
     * @return
    </a></a> */
    val sourceURL: String?
        get() {
            if (source == null)
                return null

            val pattern = Pattern.compile("\"(.*?)\"")
            val matcher = pattern.matcher(source!!)
            if (matcher.find()) {
                return matcher.group(1)
            } else {
                return source
            }
        }

    fun setSource(title: String?, url: String?) {
        if (title == null) {
            if (url == null) {
                source = null
            } else {
                source = url
            }
        } else {
            if (url == null) {
                source = title
            } else {
                source = "<a href=\"$url\">$title</a>"
            }
        }
    }

    /**
     * @return true if this Build's items are ordered chronologically (as they should be)
     */
    val isWellOrdered: Boolean
        get() = isWellOrdered(items)

    companion object {

        private const val serialVersionUID = -3970314516973612524L

        fun objectsEquivalent(lhs: Any?, rhs: Any?): Boolean {
            return lhs === rhs || lhs != null && lhs == rhs
        }

        /**
         * Checks if a list of build items are in chronological order
         * @param items
         * *
         * @return
         */
        fun isWellOrdered(items: ArrayList<BuildItem>?): Boolean {
            if (items == null || items.size <= 1)
                return true

            for (i in 1..items.size - 1) {
                if (items[i].time < items[i - 1].time)
                    return false
            }
            return true
        }
    }
}
