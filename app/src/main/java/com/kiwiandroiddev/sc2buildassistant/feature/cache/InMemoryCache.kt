package com.kiwiandroiddev.sc2buildassistant.feature.cache

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by matthome on 5/08/17.
 */
class InMemoryCache<T> : Cache<T> {

    private val keys: MutableList<String> = ArrayList()
    private val objectMap: MutableMap<String, T> = HashMap<String, T>()

    override fun put(key: String, value: T, ttlMs: Int): Completable {
        objectMap[key] = value
        keys += key
        return Completable.complete()
    }

    override fun get(key: String): Single<T> {
        if (key !in objectMap.keys) {
            return Single.error(Cache.NoValueForKey())
        }
        return Single.just(objectMap[key])
    }

    override fun clear(): Completable {
        objectMap.clear()
        return Completable.complete()
    }

    override fun remove(key: String): Completable {
        if (key in objectMap.keys) {
            objectMap.remove(key)
            return Completable.complete()
        } else {
            return Completable.error(Cache.NoValueForKey())
        }
    }

    override fun keys(): Observable<String> {
        return Observable.fromIterable(objectMap.keys)
    }

}