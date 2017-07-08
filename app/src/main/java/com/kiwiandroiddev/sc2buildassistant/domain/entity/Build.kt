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
data class Build @JvmOverloads constructor(var name: String? = null,
                 var faction: Faction? = null,
                 var vsFaction: Faction? = null,
                 var expansion: Expansion = Expansion.WOL,
                 var source: String? = null,
                 var notes: String? = null,
                 var items: ArrayList<BuildItem>? = null,
                 var created: Date? = null,
                 var modified: Date? = null,
                 var author: String? = null,
                 var isoLanguageCode: String? = null) : Serializable {

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
