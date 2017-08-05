package com.kiwiandroiddev.sc2buildassistant.feature.cache

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Created by matthome on 5/08/17.
 */
interface Cache<T> {

    fun put(key: String, value: T, ttlMs: Int = TTL_INFINITE): Completable
    fun get(key: String): Single<T>
    fun clear(): Completable
    fun remove(key: String): Completable
    fun keys(): Observable<String>

    class NoValueForKey : RuntimeException()

    companion object {
        const val TTL_INFINITE = -1
    }

}