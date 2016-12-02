package com.kiwiandroiddev.sc2buildassistant.database;

import android.database.sqlite.SQLiteDatabase;

/**
 * Patches can upgrade a database schema from one version to another.
 *
 * Created by matt on 3/12/16.
 */
interface Patch {

    void upgrade(SQLiteDatabase db);

}
