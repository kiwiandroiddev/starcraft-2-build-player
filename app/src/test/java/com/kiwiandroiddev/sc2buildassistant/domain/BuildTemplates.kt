package com.kiwiandroiddev.sc2buildassistant.domain

import com.kiwiandroiddev.sc2buildassistant.domain.entity.Build
import com.kiwiandroiddev.sc2buildassistant.domain.entity.Faction

/**
 * Created by Matt Clarke on 17/06/17.
 */
val TEST_BUILD = Build().apply {
    name = "Fast expand"
    author = "Raynor"
    source = "<a href=\"http://sc2builds\">sc2builds</a>"
    notes = "Build instructions here"
    faction = Faction.TERRAN
    vsFaction = Faction.ZERG
}