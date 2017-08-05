package com.kiwiandroiddev.sc2buildassistant.feature.cache

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by matthome on 5/08/17.
 */
class InMemoryCache<T> : Cache<T> {

    private val objectMap: MutableMap<String, T> = HashMap<String, T>()

    override fun put(key: String, value: T): Completable =
            Completable.fromAction { objectMap[key] = value }

    override fun get(key: String): Single<T> =
            Single.fromCallable { objectMap[key] ?: throw Cache.NoValueForKey() }

    override fun clear(): Completable =
            Completable.fromAction { objectMap.clear() }

    override fun remove(key: String): Completable =
            Completable.fromAction { objectMap.remove(key) ?: throw Cache.NoValueForKey() }

    override fun keys(): Observable<String> =
            Observable.fromIterable(objectMap.keys)

}