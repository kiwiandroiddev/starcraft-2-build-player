package com.kiwiandroiddev.sc2buildassistant.feature.cache

import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith

/**
 * Created by matthome on 5/08/17.
 */
@RunWith(JUnitPlatform::class)
class InMemoryCacheSpec : Spek({

    val TEST_KEY_1 = "KEY_001"
    val TEST_VALUE_1 = "test item"
    val TEST_VALUE_2 = "test item 2"

    given("an in-memory cache for storing strings") {
        var cache = InMemoryCache<String>()

        context("cache is empty") {

            beforeEachTest { cache = InMemoryCache<String>() }

            on("clear") {
                val testObserver = cache.clear()
                        .subscribeTestObserver<String>()

                it("should complete without error") {
                    testObserver
                            .assertNoErrors()
                            .assertComplete()
                }
            }

            on("keys") {
                val testObserver = cache.keys()
                        .subscribeTestObserver()

                it("should complete with no values or errors") {
                    testObserver
                            .assertNoErrors()
                            .assertNoValues()
                            .assertComplete()
                }
            }

            on("get value for some key") {
                val testObserver = cache.get(TEST_KEY_1)
                        .subscribeTestObserver()

                it("should emit a 'no value for key' error") {
                    testObserver
                            .assertError(Cache.NoValueForKey::class.java)
                            .assertNoValues()
                }
            }

            on("remove some key") {
                val testObserver = cache.remove(TEST_KEY_1)
                        .subscribeTestObserver<String>()

                it("should emit a 'no value for key' error") {
                    testObserver
                            .assertError(Cache.NoValueForKey::class.java)
                            .assertNoValues()
                }
            }

            on("put some value for some key") {
                val testObserver = cache.put(TEST_KEY_1, TEST_VALUE_1)
                        .subscribeTestObserver<String>()

                it("should complete without error") {
                    testObserver
                            .assertNoErrors()
                            .assertComplete()
                }
            }
        }

        context("cache has one stored item") {

            beforeEachTest {
                cache = InMemoryCache<String>()
                cache.put(TEST_KEY_1, TEST_VALUE_1).subscribe()
            }

            on("clear") {
                val testObserver = cache.clear()
                        .subscribeTestObserver<String>()

                it("should complete without error") {
                    testObserver
                            .assertNoErrors()
                            .assertComplete()
                }

                it("should have no keys") {
                    cache.keys()
                            .subscribeTestObserver()
                            .assertNoValues()
                            .assertNoErrors()
                            .assertComplete()
                }

                it("should throw NoValueForKey if get is called for some key") {
                    cache.get(TEST_KEY_1)
                            .subscribeTestObserver()
                            .assertError(Cache.NoValueForKey::class.java)
                }
            }

            on("keys") {
                val testObserver = cache.keys()
                        .subscribeTestObserver()

                it("should emit the single expected key") {
                    testObserver.assertResult(TEST_KEY_1)
                }
            }

            on("remove the key") {
                val testObserver = cache.remove(TEST_KEY_1)
                        .subscribeTestObserver<String>()

                it("should complete without error") {
                    testObserver
                            .assertNoErrors()
                            .assertComplete()
                }

                it("should throw NoValueForKey if get is called for that key") {
                    cache.get(TEST_KEY_1)
                            .subscribeTestObserver()
                            .assertError(Cache.NoValueForKey::class.java)
                }
            }

            on("get value for the key") {
                val testObserver = cache.get(TEST_KEY_1)
                        .subscribeTestObserver()

                it("should emit the stored value") {
                    testObserver.assertResult(TEST_VALUE_1)
                }
            }

            on("put new value with the same key") {
                val testObserver = cache.put(TEST_KEY_1, TEST_VALUE_2)
                        .subscribeTestObserver<String>()

                it("should complete without error") {
                    testObserver
                            .assertNoErrors()
                            .assertComplete()
                }

                it("should now contain the new value for that key") {
                    cache.get(TEST_KEY_1)
                            .subscribeTestObserver()
                            .assertResult(TEST_VALUE_2)
                }
            }
        }

        context("cache had one item added then removed") {
            cache.put(TEST_KEY_1, TEST_VALUE_1)
                    .andThen(cache.remove(TEST_KEY_1))
                    .subscribe()


        }
    }
})
