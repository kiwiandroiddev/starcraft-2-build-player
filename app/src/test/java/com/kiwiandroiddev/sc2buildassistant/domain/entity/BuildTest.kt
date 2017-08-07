package com.kiwiandroiddev.sc2buildassistant.domain.entity

import org.assertj.core.api.Assertions.*
import org.junit.Before
import org.junit.Test

/**
 * Created by matthome on 8/07/17.
 */
class BuildTest {

    lateinit var build: Build

    @Before
    fun setUp() {
        build = Build()
    }

    private fun givenABuildWithItems(vararg items: BuildItem) {
        build.items = ArrayList(items.asList())
    }

    private fun givenABuildWithAnHtmlLinkSource(url: String, text: String) {
        build.source = "<a href=\"$url\">$text<\\a>"
    }

    @Test
    fun isWellOrdered_noBuildItems_returnsTrue() {
        assertThat(build.isWellOrdered).isTrue()
    }

    @Test
    fun isWellOrdered_oneBuildItem_returnsTrue() {
        givenABuildWithItems(BuildItem(1, "SCV"))

        assertThat(build.isWellOrdered).isTrue()
    }

    @Test
    fun isWellOrdered_twoBuildItemsOrderedByTime_returnsTrue() {
        givenABuildWithItems(BuildItem(1, "SCV"), BuildItem(2, "SCV"))

        assertThat(build.isWellOrdered).isTrue()
    }

    @Test
    fun isWellOrdered_twoBuildItemsWithLaterTimeItemFirst_returnsFalse() {
        givenABuildWithItems(BuildItem(2, "SCV"), BuildItem(1, "SCV"))

        assertThat(build.isWellOrdered).isFalse()
    }

    @Test
    fun isWellOrdered_twoBuildItemsWithSameTime_returnsTrue() {
        givenABuildWithItems(BuildItem(1, "SCV"), BuildItem(1, "SCV"))

        assertThat(build.isWellOrdered).isTrue()
    }

    @Test
    fun getSourceTitle_noSourceSet_returnsNull() {
        assertThat(build.sourceTitle).isNull()
    }

    @Test
    fun getSourceTitle_sourceIsEmptyString_returnsEmptyString() {
        build.source = ""

        assertThat(build.sourceTitle).isEqualTo("")
    }

    @Test
    fun getSourceTitle_sourceIsNonHtmlString_returnsSource() {
        build.source = "Liquidpedia"

        assertThat(build.sourceTitle).isEqualTo("Liquidpedia")
    }

    @Test
    fun getSourceTitle_sourceIsAnHtmlLink_returnsSourceTextWithoutHtml() {
        givenABuildWithAnHtmlLinkSource(url = "http://wiki.teamliquid.net", text = "Liquidpedia")

        assertThat(build.sourceTitle).isEqualTo("Liquidpedia")
    }

    @Test
    fun getSourceUrl_sourceIsAnHtmlLink_returnsUrlOnly() {
        givenABuildWithAnHtmlLinkSource(url = "http://wiki.teamliquid.net", text = "Liquidpedia")

        assertThat(build.sourceURL).isEqualTo("http://wiki.teamliquid.net")
    }

}