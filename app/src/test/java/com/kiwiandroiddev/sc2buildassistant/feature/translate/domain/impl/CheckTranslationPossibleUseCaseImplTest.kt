package com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.impl

import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.CheckTranslationPossibleUseCase
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.LanguageCode
import com.kiwiandroiddev.sc2buildassistant.feature.translate.domain.datainterface.SupportedLanguagesAgent
import com.kiwiandroiddev.sc2buildassistant.subscribeTestObserver
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

/**
 * Created by matthome on 31/07/17.
 */
class CheckTranslationPossibleUseCaseImplTest {

    @Mock lateinit var mockSupportedLanguagesAgent: SupportedLanguagesAgent

    lateinit var checkTranslationPossibleUseCase: CheckTranslationPossibleUseCase

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        checkTranslationPossibleUseCase =
                CheckTranslationPossibleUseCaseImpl(mockSupportedLanguagesAgent)
    }

    private fun givenSupportedLanguagesAgentThrowsError(error: RuntimeException) {
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.error(error))
    }

    private fun givenSupportedLanguageCodes(vararg languageCodes: LanguageCode) {
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.fromArray(*languageCodes))
    }

    private fun givenNoLanguageCodesAreSupported() {
        `when`(mockSupportedLanguagesAgent.supportedLanguages())
                .thenReturn(Observable.empty())
    }

    @Test
    fun fromDeToEn_getSupportedLanguagesAgentThrowsError_returnsFalse() {
        val error = RuntimeException("No network connection!")
        givenSupportedLanguagesAgentThrowsError(error)

        checkTranslationPossibleUseCase
                .canTranslateFromLanguage(fromLanguageCode = "de", toLanguageCode = "en")
                .subscribeTestObserver()
                .assertResult(false)
    }

    @Test
    fun fromDeToEn_getSupportedLanguagesAgentReturnsEmptyList_returnsFalse() {
        givenNoLanguageCodesAreSupported()

        checkTranslationPossibleUseCase
                .canTranslateFromLanguage(fromLanguageCode = "de", toLanguageCode = "en")
                .subscribeTestObserver()
                .assertResult(false)
    }

    @Test
    fun fromDeToEn_getSupportedLanguagesAgentReturnsEnAndDe_returnsTrue() {
        givenSupportedLanguageCodes("de", "en")

        checkTranslationPossibleUseCase
                .canTranslateFromLanguage(fromLanguageCode = "de", toLanguageCode = "en")
                .subscribeTestObserver()
                .assertResult(true)
    }

    @Test
    fun fromEsToRu_getSupportedLanguagesAgentReturnsEnAndDe_returnsFalse() {
        givenSupportedLanguageCodes("de", "en")

        checkTranslationPossibleUseCase
                .canTranslateFromLanguage(fromLanguageCode = "es", toLanguageCode = "ru")
                .subscribeTestObserver()
                .assertResult(false)
    }

    @Test
    fun fromEsToRu_getSupportedLanguagesAgentReturnsEnDeEsRu_returnsTrue() {
        givenSupportedLanguageCodes("en", "de", "es", "ru")

        checkTranslationPossibleUseCase
                .canTranslateFromLanguage(fromLanguageCode = "es", toLanguageCode = "ru")
                .subscribeTestObserver()
                .assertResult(true)
    }

}
