package com.kiwiandroiddev.sc2buildassistant.feature.translate.data

import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache.CachedSupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test

import org.junit.Before
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * Created by matthome on 6/08/17.
 */
class CachedSupportedLanguagesAgentTest {

    @Mock lateinit var mockSupportedLanguagesAgent: SupportedLanguagesAgent
    @Mock lateinit var mockCache: Cache<List<LanguageCode>>

    lateinit var cachedSupportedLanguagesAgent: SupportedLanguagesAgent

    val EXPECTED_CACHE_KEY = "SUPPORTED_LANGUAGES"

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        cachedSupportedLanguagesAgent = CachedSupportedLanguagesAgent(
                mockSupportedLanguagesAgent,
                mockCache
        )

        setupDefaultMockBehaviors()
    }

    private fun setupDefaultMockBehaviors() {
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.empty())
    }

    @Test
    fun supportedLanguages_cacheMiss_getsFromBackingAgentAndPutsInCache() {
        val TEST_LANGUAGE_CODES = listOf("en", "de", "zh")
        var cacheUpdated = false
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.fromIterable(TEST_LANGUAGE_CODES))
        `when`(mockCache.get(EXPECTED_CACHE_KEY))
                .thenReturn(Single.error(Cache.NoValueForKey()))
        `when`(mockCache.put(com.nhaarman.mockito_kotlin.any(), com.nhaarman.mockito_kotlin.any()))
                .thenReturn(Completable.fromAction { cacheUpdated = true })

        cachedSupportedLanguagesAgent.supportedLanguages().subscribeTestObserver()

        verify(mockCache).get(EXPECTED_CACHE_KEY)
        verify(mockSupportedLanguagesAgent).supportedLanguages()
        assertThat(cacheUpdated).isTrue()
        verify(mockCache).put(EXPECTED_CACHE_KEY, TEST_LANGUAGE_CODES)
    }

    @Test
    fun supportedLanguages_cacheHit_doesntHitBackingAgent() {
        `when`(mockCache.get(EXPECTED_CACHE_KEY))
                .thenReturn(Single.just(listOf("en", "de", "zh")))

        cachedSupportedLanguagesAgent.supportedLanguages().subscribeTestObserver()

        verify(mockCache).get(EXPECTED_CACHE_KEY)
        verify(mockSupportedLanguagesAgent, never()).supportedLanguages()
    }

    @Test
    fun supportedLanguages_cacheMissAndCachePutWillFail_recoversByJustReturningCodesFromBackingAgent() {
        val TEST_LANGUAGE_CODES = listOf("en", "de", "zh")
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.fromIterable(TEST_LANGUAGE_CODES))
        `when`(mockCache.get(EXPECTED_CACHE_KEY))
                .thenReturn(Single.error(Cache.NoValueForKey()))
        `when`(mockCache.put(com.nhaarman.mockito_kotlin.any(), com.nhaarman.mockito_kotlin.any()))
                .thenReturn(Completable.error(IOException("disk full")))

        cachedSupportedLanguagesAgent.supportedLanguages().subscribeTestObserver()

        verify(mockCache).get(EXPECTED_CACHE_KEY)
        verify(mockSupportedLanguagesAgent).supportedLanguages()
        verify(mockCache).put(EXPECTED_CACHE_KEY, TEST_LANGUAGE_CODES)
    }

}