package com.kiwiandroiddev.sc2buildassistant.feature.translate.data.cache

import com.kiwiandroiddev.sc2buildassistant.feature.cache.Cache
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.TranslationAgent
import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test

import org.junit.Assert.*
import org.junit.Before
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.IOException

/**
 * Created by matthome on 6/08/17.
 */
class CachedTranslationAgentTest {

    @Mock lateinit var mockTranslationCache: Cache<String>
    @Mock lateinit var mockTranslationAgent: TranslationAgent

    lateinit var cachedTranslationAgent: TranslationAgent

    private val SAMPLE_TEXT_1 = "Lorem ipsum dolor"
    private val SAMPLE_TRANSLATION_1 = "translated text here"

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        cachedTranslationAgent = CachedTranslationAgent(
                mockTranslationAgent,
                mockTranslationCache
        )

        setupDefaultMockBehaviors()
    }

    private fun setupDefaultMockBehaviors() {
        `when`(mockTranslationAgent.getTranslation(any(), any(), any()))
                .thenReturn(Single.just(SAMPLE_TRANSLATION_1))
    }

    @Test
    fun getTranslation_cacheMiss_getsTranslationFromBackingAgentAndUpdatesCache() {
        var cacheUpdated = false
        `when`(mockTranslationCache.put(any(), any()))
                .thenReturn(Completable.fromAction { cacheUpdated = true })
        `when`(mockTranslationCache.get(any()))
                .thenReturn(Single.error(Cache.NoValueForKey()))

        cachedTranslationAgent.getTranslation("en", "de", SAMPLE_TEXT_1)
                .subscribeTestObserver()
                .assertResult(SAMPLE_TRANSLATION_1)

        verify(mockTranslationCache).get(any())
        verify(mockTranslationAgent).getTranslation("en", "de", SAMPLE_TEXT_1)
        verify(mockTranslationCache).put(any(), any())
        assertThat(cacheUpdated).isTrue()
    }

    @Test
    fun getTranslation_cacheMissAndCacheUpdateWillFail_recoversByJustGettingTranslationFromBackingAgent() {
        `when`(mockTranslationCache.put(any(), any()))
                .thenReturn(Completable.error(IOException("disk full")))
        `when`(mockTranslationCache.get(any()))
                .thenReturn(Single.error(Cache.NoValueForKey()))

        cachedTranslationAgent.getTranslation("en", "de", SAMPLE_TEXT_1)
                .subscribeTestObserver()
                .assertNoErrors()
                .assertResult(SAMPLE_TRANSLATION_1)

        verify(mockTranslationCache).get(any())
        verify(mockTranslationAgent).getTranslation("en", "de", SAMPLE_TEXT_1)
        verify(mockTranslationCache).put(any(), any())
    }

    @Test
    fun getTranslation_cacheHit_doesntHitBackingAgent() {
        `when`(mockTranslationCache.get(any()))
                .thenReturn(Single.just(SAMPLE_TRANSLATION_1))

        cachedTranslationAgent.getTranslation("en", "de", SAMPLE_TEXT_1)
                .subscribeTestObserver()
                .assertResult(SAMPLE_TRANSLATION_1)

        verify(mockTranslationCache).get(any())
        verify(mockTranslationAgent, never()).getTranslation(any(), any(), any())
        verify(mockTranslationCache, never()).put(any(), any())
    }

    @Test
    fun getTranslation_cacheMiss_cacheKeyUsedIsHashOfTranslationRequestParameters() {
        `when`(mockTranslationCache.get(any()))
                .thenReturn(Single.error(Cache.NoValueForKey()))

        cachedTranslationAgent.getTranslation("en", "de", SAMPLE_TEXT_1)
                .subscribeTestObserver()

        val expectedKey = Triple("en", "de", SAMPLE_TEXT_1).hashCode().toString()
        verify(mockTranslationCache).get(expectedKey)
        verify(mockTranslationCache).put(expectedKey, SAMPLE_TRANSLATION_1)
    }
}