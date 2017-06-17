package com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.impl

import com.kiwiandroiddev.sc2buildassistant.domain.TEST_BUILD
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.GetBuildUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.brief.domain.datainterface.BuildRepository
import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Created by Matt Clarke on 17/06/17.
 */
class GetBuildUseCaseImplTest {

    @Mock lateinit var mockBuildRepository: BuildRepository

    lateinit var getBuildUseCase: GetBuildUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        getBuildUseCase = GetBuildUseCaseImpl(mockBuildRepository)
    }

    @Test
    fun getBuild_repositoryReturnsBuildForId1_returnsBuild() {
        `when`(mockBuildRepository.getBuildForId(buildId = 1L)).thenReturn(Single.just(TEST_BUILD))

        val testObserver = getBuildUseCase.getBuild(1L).subscribeTestObserver()

        testObserver.assertResult(TEST_BUILD)
    }

    @Test
    fun getBuild_repositoryThrowsError_rethrowsError() {
        `when`(mockBuildRepository.getBuildForId(buildId = 2L)).thenReturn(Single.error(RuntimeException()))

        val testObserver = getBuildUseCase.getBuild(2L).subscribeTestObserver()

        testObserver.assertError(RuntimeException::class.java)
    }

}

