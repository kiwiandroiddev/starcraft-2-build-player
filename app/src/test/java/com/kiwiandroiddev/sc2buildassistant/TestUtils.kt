package com.kiwiandroiddev.sc2buildassistant

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.observers.TestObserver

/**
 * Created by Matt Clarke on 17/06/17.
 */
fun <T> Observable<T>.subscribeTestObserver(): TestObserver<T> {
    val testObserver: TestObserver<T> = TestObserver.create<T>()
    this.subscribe(testObserver)
    return testObserver
}

fun <T> Single<T>.subscribeTestObserver(): TestObserver<T> {
    val testObserver: TestObserver<T> = TestObserver.create<T>()
    this.subscribe(testObserver)
    return testObserver
}

fun <T> Completable.subscribeTestObserver(): TestObserver<T> {
    val testObserver: TestObserver<T> = TestObserver.create()
    this.subscribe(testObserver)
    return testObserver
}