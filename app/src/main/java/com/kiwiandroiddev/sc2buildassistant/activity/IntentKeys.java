package com.kiwiandroiddev.sc2buildassistant.activity;

/**
 * Contains all Intent Extra key constants used for passing data across activities
 * throughout the app.
 *
 * Created by matt on 21/06/15.
 */
public final class IntentKeys {
    // keys that this activity uses when passing data to other activities or fragments
    public static final String KEY_BUILD_ID = "com.kiwiandroiddev.sc2buildassistant.BuildId";
    public static final String KEY_BUILD_NAME = "com.kiwiandroiddev.sc2buildassistant.BuildName";
    public static final String KEY_EXPANSION_ENUM = "com.kiwiandroiddev.sc2buildassistant.Expansion";
    public static final String KEY_FACTION_ENUM = "com.kiwiandroiddev.sc2buildassistant.Faction";
    public static final String KEY_ITEM_TYPE_ENUM = "com.kiwiandroiddev.sc2buildassistant.ItemType";
    public static final String KEY_BUILD_OBJECT = "com.kiwiandroiddev.sc2buildassistant.domain.model.Build";
    public static final String KEY_BUILD_ITEM_OBJECT = "com.kiwiandroiddev.sc2buildassistant.domain.model.BuildItem";
}
