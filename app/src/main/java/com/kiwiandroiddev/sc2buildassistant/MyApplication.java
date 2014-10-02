package com.kiwiandroiddev.sc2buildassistant;


import android.app.Application;

/**
 * Makes a database instance visible across the application, to prevent frequent opening
 * and closing of a new database in every class
 * 
 * @author matt
 *
 */
public class MyApplication extends Application {
	  private DbAdapter mDb;

	  public DbAdapter getDb() {
		  if (mDb == null)
			  mDb = new DbAdapter(getApplicationContext());
		  return mDb;
	  }
}
