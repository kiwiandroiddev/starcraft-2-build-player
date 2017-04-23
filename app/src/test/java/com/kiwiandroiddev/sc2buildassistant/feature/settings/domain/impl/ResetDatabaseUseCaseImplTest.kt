package com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ClearDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.LoadStandardBuildsIntoDatabaseUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.settings.domain.ResetDatabaseUseCase
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.observers.TestObserver
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class ResetDatabaseUseCaseImplTest {

    @Mock lateinit var mockClearDatabaseUseCase: ClearDatabaseUseCase
    @Mock lateinit var mockLoadStandardBuildsIntoDatabaseUseCase: LoadStandardBuildsIntoDatabaseUseCase

    lateinit var testObserver: TestObserver<Void>

    lateinit var resetDatabaseUseCase: ResetDatabaseUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        testObserver = TestObserver.create()

        resetDatabaseUseCase = ResetDatabaseUseCaseImpl(
                mockClearDatabaseUseCase,
                mockLoadStandardBuildsIntoDatabaseUseCase)

        initDefaultMockBehaviours()
    }

    private fun initDefaultMockBehaviours() {
        `when`(mockClearDatabaseUseCase.clear()).thenReturn(Completable.complete())
        `when`(mockLoadStandardBuildsIntoDatabaseUseCase.loadBuilds())
                .thenReturn(Observable.just(100))
    }

    private fun resetDatabase() {
        resetDatabaseUseCase.resetDatabase().subscribe(testObserver)
    }

    @Test
    fun resetDatabase_clearDatabaseAndLoadStandardBuildsWillSucceed_completesWithNoErrors() {
        resetDatabase()

        with(testObserver) {
            assertNoErrors()
            assertComplete()
        }
    }

    @Test
    fun resetDatabase_clearDatabaseFails_shouldFail() {
        `when`(mockClearDatabaseUseCase.clear())
                .thenReturn(Completable.error(RuntimeException("IO error")))

        resetDatabase()

        with(testObserver) {
            assertError(RuntimeException::class.java)
            assertNotComplete()
        }
    }

    @Test
    fun resetDatabase_loadStandardBuildsFails_shouldFail() {
        val error = RuntimeException("IO error")
        `when`(mockLoadStandardBuildsIntoDatabaseUseCase.loadBuilds())
                .thenReturn(Observable.error(error))

        resetDatabase()

        with(testObserver) {
            assertError(error)
            assertNotComplete()
        }
    }
}
