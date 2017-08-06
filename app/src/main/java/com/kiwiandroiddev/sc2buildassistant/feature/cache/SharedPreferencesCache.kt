package com.kiwiandroiddev.sc2buildassistant.feature.cache

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.Serializable

/**
 * Created by matthome on 6/08/17.
 */
@SuppressLint("ApplySharedPref")
class SharedPreferencesCache<T : Serializable>(val context: Context,
                                               val name: String,
                                               val classOfT: Class<T>) : Cache<T> {

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    private val gson: Gson by lazy { Gson() }

    private fun T.toJsonString(): String = gson.toJson(this)

    override fun put(key: String, value: T): Completable =
            Completable.fromAction {
                prefs.edit()
                        .putString(key, value.toJsonString())
                        .commit()
            }

    override fun get(key: String): Single<T> =
            Single.fromCallable {
                if (!prefs.contains(key)) throw Cache.NoValueForKey()

                val serializedObject = prefs.getString(key, "")
                gson.fromJson(serializedObject, classOfT)
            }

    override fun clear(): Completable =
            Completable.fromAction { prefs.edit().clear().commit() }

    override fun remove(key: String): Completable =
            Completable.fromAction { prefs.edit().remove(key).commit() }

    override fun keys(): Observable<String> =
            Observable.fromIterable(prefs.all.keys)

}