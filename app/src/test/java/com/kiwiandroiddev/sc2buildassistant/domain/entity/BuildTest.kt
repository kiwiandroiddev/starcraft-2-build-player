package com.kiwiandroiddev.sc2buildassistant.domain.entity

import org.assertj.core.api.Assertions.*
import org.junit.Test

/**
 * Created by matthome on 8/07/17.
 */
class BuildTest {

    lateinit var build: Build

    private fun givenABuildWithNoItems() {
        givenABuildWithItems()
    }

    private fun givenABuildWithItems(vararg items: BuildItem) {
        build = Build()
        build.items = ArrayList(items.asList())
    }

    @Test
    fun isWellOrdered_noBuildItems_returnsTrue() {
        givenABuildWithNoItems()

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

}