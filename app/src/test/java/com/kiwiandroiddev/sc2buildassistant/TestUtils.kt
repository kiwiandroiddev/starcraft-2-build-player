package com.kiwiandroiddev.sc2buildassistant

import io.reactivex.Observable
import io.reactivex.observers.TestObserver

/**
 * Created by Matt Clarke on 17/06/17.
 */
fun <T> Observable<T>.subscribeTestObserver(): TestObserver<T> {
    val testObserver: TestObserver<T> = TestObserver.create<T>()
    this.subscribe(testObserver)
    return testObserver
}