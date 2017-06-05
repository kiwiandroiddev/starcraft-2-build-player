package com.kiwiandroiddev.sc2buildassistant.feature.buildlist.presentation.model

/**
 * Created by Matt Clarke on 5/06/17.
 */
data class BuildViewModel(val buildId: Long,
                          val name: String,
                          val creationDate: String,
                          val vsRace: String)
