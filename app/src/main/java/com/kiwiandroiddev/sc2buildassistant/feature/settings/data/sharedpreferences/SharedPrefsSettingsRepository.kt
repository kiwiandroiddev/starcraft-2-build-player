package com.kiwiandroiddev.sc2buildassistant.feature.settings.data.sharedpreferences

import android.content.SharedPreferences
import com.kiwiandroiddev.sc2buildassistant.feature.settings.data.sharedpreferences.SettingKeys.KEY_SHOW_ADS
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.datainterface.SettingsRepository
import io.reactivex.Observable

/**
 * Created by Matt Clarke on 17/06/17.
 */
class SharedPrefsSettingsRepository(val sharedPreferences: SharedPreferences) : SettingsRepository {

    override fun showAds(): Observable<Boolean> =
            Observable.just(sharedPreferences.getBoolean(KEY_SHOW_ADS, false))

}