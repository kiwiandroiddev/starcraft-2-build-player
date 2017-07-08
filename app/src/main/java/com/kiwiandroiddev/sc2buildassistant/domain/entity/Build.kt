package com.kiwiandroiddev.sc2buildassistant.domain.entity

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.ArrayList
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * Encapsulates all information about a particular build order
 * including its name, the race it's for, and the units with timestamps
 */
data class Build @JvmOverloads constructor(
        @SerializedName("mName") var name: String? = null,
        @SerializedName("mFaction") var faction: Faction? = null,
        @SerializedName("mVsFaction") var vsFaction: Faction? = null,
        @SerializedName("mExpansion") var expansion: Expansion = Expansion.WOL,
        @SerializedName("mSource") var source: String? = null,
        @SerializedName("mNotes") var notes: String? = null,
        @SerializedName("mItems") var items: ArrayList<BuildItem>? = null,
        @SerializedName("mCreated") var created: Date? = null,
        @SerializedName("mModified") var modified: Date? = null,
        @SerializedName("mAuthor") var author: String? = null,
        @SerializedName("mIsoLanguageCode") var isoLanguageCode: String? = null) : Serializable {

    /**
     * Assuming the source string is an html link (<a> tag), returns the plain text component
     * If it's not an <a> tag, return the full source string
     */
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
     */
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
