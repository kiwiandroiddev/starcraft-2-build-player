package com.kiwiandroiddev.sc2buildassistant.test;

import junit.framework.Assert;

import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter.ItemType;

import android.test.AndroidTestCase;
import android.util.Log;

public class BuildDBAdapterTest extends AndroidTestCase {

	public static final String TAG = "BuildDBAdapterTest";
	
	private DbAdapter mDb;
	
	@Override
	protected void setUp() {
		Log.d(TAG, "setUp() called");
		mDb = new DbAdapter(this.getContext());
		mDb.open();
		mDb.clear();
	}
	
	@Override
	protected void tearDown() {
		Log.d(TAG, "tearDown() called");
		mDb.close();
		mDb = null;
	}
		
	public void testGetAllBuilds() throws Throwable {

	}
	
//	public void testAddNewBuild() throws Throwable {
//		mDb.addBuild(new Build("awesome BO", DbAdapter.Faction.TERRAN, new BuildItem[] {}));
//		
//		Gson gson = new Gson();
//		JsonReader reader = new JsonReader(new InputStreamReader(mContext.getAssets().open("builds/std_terran_builds.json")));
//		Build[] builds = gson.fromJson(reader, Build[].class);
//		mDb.addBuild(builds[0]);
//	}
	
	public void testGetItemTypeID() throws Throwable {
		Assert.assertEquals(mDb.getItemTypeID(ItemType.UNIT), 1);
		Assert.assertEquals(mDb.getItemTypeID(ItemType.STRUCTURE), 2);
		Assert.assertEquals(mDb.getItemTypeID(ItemType.UPGRADE), 3);
		Assert.assertEquals(mDb.getItemTypeID(ItemType.ABILITY), 4);
		Assert.assertEquals(mDb.getItemTypeID(ItemType.NOTE), 5);
	}
}
