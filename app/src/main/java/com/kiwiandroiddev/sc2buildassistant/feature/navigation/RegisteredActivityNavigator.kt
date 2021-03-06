package com.kiwiandroiddev.sc2buildassistant.feature.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.kiwiandroiddev.sc2buildassistant.R
import com.kiwiandroiddev.sc2buildassistant.activity.EditBuildActivity
import com.kiwiandroiddev.sc2buildassistant.activity.IntentKeys.KEY_BUILD_ID
import com.kiwiandroiddev.sc2buildassistant.feature.brief.presentation.BriefNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.player.view.PlaybackActivity
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.feature.settings.view.SettingsActivity
import com.kiwiandroiddev.sc2buildassistant.util.ChangeLog
import com.kiwiandroiddev.sc2buildassistant.util.EasyTrackerUtils

class RegisteredActivityNavigator : SettingsNavigator, BriefNavigator {

    companion object {
        val TRANSLATE_URL = "http://www.getlocalization.com/sc2buildplayer/"
        val PROJECT_PAGE_URL = "https://github.com/kiwiandroiddev/starcraft-2-build-player"
    }

    var activity: Activity? = null
        private set

    fun registerCurrentActivity(activity: Activity) {
        this.activity = activity
    }

    fun unregisterCurrentActivity(activity: Activity) {
        if (this.activity == activity) {
            this.activity = null
        }
    }

    override fun openTranslationPage() {
        activity?.apply {
            openUrl(TRANSLATE_URL)

            EasyTrackerUtils.sendEvent(this, "ui_action", "menu_select", "translate_option", null)
        }
    }

    override fun openFullChangelog() {
        activity?.apply {
            ChangeLog(activity).showFullLogDialog()
        }
    }

    override fun openPlayStoreListing() {
        activity?.apply {
            val playStoreUri = Uri.parse("market://details?id=" + packageName)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, playStoreUri))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(activity, R.string.dlg_no_market_error, Toast.LENGTH_SHORT).show()
            }

            EasyTrackerUtils.sendEvent(this, "ui_action", "menu_select", "rate_option", null)
        }
    }

    override fun openProjectPage() {
        activity?.apply { openUrl(PROJECT_PAGE_URL) }
    }

    private fun Activity.openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    override fun onPlayBuild(buildId: Long) {
        activity?.apply {
            val i = Intent(this, PlaybackActivity::class.java)
            i.putExtra(KEY_BUILD_ID, buildId)
            startActivity(i)
        }
    }

    override fun onEditBuild(buildId: Long) {
        activity?.apply {
            val i = Intent(this, EditBuildActivity::class.java)
            i.putExtra(KEY_BUILD_ID, buildId)
            startActivity(i)
        }
    }

    override fun onOpenSettings() {
        activity?.apply {
            val i = Intent(this, SettingsActivity::class.java)
            startActivity(i)
        }
    }

}