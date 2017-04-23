package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ResetDatabaseUseCaseImplTest {

    @Mock lateinit var mockClearDatabaseUseCase: ClearDatabaseUseCase

    lateinit var testObserver: TestObserver<Void>

    lateinit var resetDatabaseUseCase: ResetDatabaseUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        testObserver = TestObserver.create()

        resetDatabaseUseCase = ResetDatabaseUseCaseImpl(mockClearDatabaseUseCase)
    }

    @Test
    fun resetDatabase_clearDatabaseWillSucceed_completesWithNoErrors() {
        `when`(mockClearDatabaseUseCase.clear()).thenReturn(Completable.complete())

        resetDatabase()

        with(testObserver) {
            assertNoErrors()
            assertComplete()
        }
    }

    @Test
    fun resetDatabase_clearDatabaseFails_shouldFail() {
        `when`(mockClearDatabaseUseCase.clear()).thenReturn(Completable.error(RuntimeException("IO error")))

        resetDatabase()

        with(testObserver) {
            assertError(RuntimeException::class.java)
            assertNotComplete()
        }
    }

    private fun resetDatabase() {
        resetDatabaseUseCase.resetDatabase().subscribe(testObserver)
    }
}

