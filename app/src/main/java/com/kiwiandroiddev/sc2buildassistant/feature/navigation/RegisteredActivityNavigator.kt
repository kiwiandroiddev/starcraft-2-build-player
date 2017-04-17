package com.kiwiandroiddev.sc2buildassistant.feature.navigation

import android.app.Activity
import com.kiwiandroiddev.sc2buildassistant.feature.settings.presentation.SettingsNavigator
import com.kiwiandroiddev.sc2buildassistant.util.ChangeLog

class RegisteredActivityNavigator : SettingsNavigator {

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

    override fun openUrl(url: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun openFullChangelog() {
        activity?.apply {
            ChangeLog(activity).showFullLogDialog()
        }
    }

    override fun openPlayStoreListing() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}