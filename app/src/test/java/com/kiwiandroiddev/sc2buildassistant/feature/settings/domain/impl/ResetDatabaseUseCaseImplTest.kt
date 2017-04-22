package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import rx.Observable
import rx.observers.TestSubscriber

/**
 * Copyright © 2017. Orion Health. All rights reserved.
 */
class ResetDatabaseUseCaseImplTest {

    @Mock lateinit var mockClearDatabaseUseCase: ClearDatabaseUseCase

    lateinit var testSubscriber: TestSubscriber<Void>

    lateinit var resetDatabaseUseCase: ResetDatabaseUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        testSubscriber = TestSubscriber()

        resetDatabaseUseCase = ResetDatabaseUseCaseImpl(mockClearDatabaseUseCase)
    }

    @Test
    fun resetDatabase_clearDatabaseWillSucceed_completesWithNoErrors() {
        `when`(mockClearDatabaseUseCase.clear()).thenReturn(Observable.just(null))
        
        resetDatabase()

        with(testSubscriber) {
            assertNoErrors()
            assertCompleted()
            assertValue(null)
        }
    }

    @Test
    fun resetDatabase_clearDatabaseFails_shouldFail() {
        `when`(mockClearDatabaseUseCase.clear()).thenReturn(Observable.error(RuntimeException("IO error")))

        resetDatabase()

        with(testSubscriber) {
            assertError(RuntimeException::class.java)
            assertNotCompleted()
            assertNoValues()
        }
    }

    private fun resetDatabase() {
        resetDatabaseUseCase.resetDatabase().subscribe(testSubscriber)
    }
}

