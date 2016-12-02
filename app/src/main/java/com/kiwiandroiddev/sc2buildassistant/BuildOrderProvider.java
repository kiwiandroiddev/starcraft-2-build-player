package com.kiwiandroiddev.sc2buildassistant;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.kiwiandroiddev.sc2buildassistant.database.DbAdapter;

import java.util.HashMap;

/**
 * Content provider for accessing the build order table only (at this stage).
 * 
 * This class only really exists so a CursorLoader can be used to populate and
 * manage the listviews of builds in each RaceFragment. For accessing the database
 * internally, DbAdapter is usually a better choice as it contains a number of
 * data access helpers (no need to mess around with low level SQL queries).
 * 
 * @author matt
 *
 */
public class BuildOrderProvider extends ContentProvider {

	public static final String AUTHORITY = "com.kiwiandroiddev.sc2buildassistant.buildorderprovider";
	public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);
	
	private static final String CONTENT_TYPE_ONE_ROW = ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.kiwiandroiddev.build_order";
	private static final String CONTENT_TYPE_MULTIPLE_ROWS = ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.kiwiandroiddev.build_order";
	
	private static final int URI_BO_TABLE_ID = 0;
	private static final int URI_BO_ROW_ID = 1;
    
	private static final UriMatcher sUriMatcher;
	private static final HashMap<String, String> sBuildOrderProjectionMap;
	
	private DbAdapter mDb;
	
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "build_order", URI_BO_TABLE_ID);
		sUriMatcher.addURI(AUTHORITY, "build_order/#", URI_BO_ROW_ID);
		
		// seems pointless but is needed by query builder
		sBuildOrderProjectionMap = new HashMap<String, String>();
		sBuildOrderProjectionMap.put(DbAdapter.KEY_BUILD_ORDER_ID, DbAdapter.KEY_BUILD_ORDER_ID);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_NAME, DbAdapter.KEY_NAME);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_EXPANSION_ID, DbAdapter.KEY_EXPANSION_ID);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_FACTION_ID, DbAdapter.KEY_FACTION_ID);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_VS_FACTION_ID, DbAdapter.KEY_VS_FACTION_ID);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_SOURCE, DbAdapter.KEY_SOURCE);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_AUTHOR, DbAdapter.KEY_AUTHOR);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_DESCRIPTION, DbAdapter.KEY_DESCRIPTION);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_CREATED, DbAdapter.KEY_CREATED);
		sBuildOrderProjectionMap.put(DbAdapter.KEY_MODIFIED, DbAdapter.KEY_MODIFIED);
	}
	
	@Override
	public boolean onCreate() {
		mDb = new DbAdapter(getContext());
		return true;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
//		Timber.d(this.toString(), "In BuildOrderProvider, delete() called with uri " + uri);
		
		// Not allowing the whole table to be deleted at this stage
		if (sUriMatcher.match(uri) == URI_BO_ROW_ID) {
            /*
             * Because this URI was for a single row, the _ID value part is
             * present. Get the last path segment from the URI; this is the _ID value.
             * Then, append the value to the WHERE clause for the query
             */
        	SQLiteDatabase db = mDb.getWritableDatabase();
            String whereClause = DbAdapter.KEY_BUILD_ORDER_ID + " = " + uri.getLastPathSegment();
            int rowsDeleted = db.delete(DbAdapter.TABLE_BUILD_ORDER, whereClause, null);
            
            // notify observers of this table that rows have been deleted
            if (rowsDeleted != 0)
            	getContext().getContentResolver().notifyChange(uri, null);
            
            return rowsDeleted;
		} else {
            // If the URI is not recognized, you should do some error handling here.
        	throw new IllegalArgumentException("URI not recognised: " + uri.toString());
		}
	}

	@Override
	public String getType(Uri uri) {
	   /**
	    * Chooses the MIME type based on the incoming URI pattern
	    */
	   switch (sUriMatcher.match(uri)) {
	
	        case URI_BO_TABLE_ID:
	            return CONTENT_TYPE_MULTIPLE_ROWS;
	 
	        case URI_BO_ROW_ID:
	            return CONTENT_TYPE_ONE_ROW;
	 
	        // If the URI pattern doesn't match any permitted patterns, throws an exception.
	        default:
	            throw new IllegalArgumentException("Unknown URI " + uri);
	    }
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		// Constructs a new query builder and sets its table name
	    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
	    qb.setTables(DbAdapter.TABLE_BUILD_ORDER);
	    
        /*
         * Choose the table to query and a sort order based on the code returned for the incoming
         * URI.
         */
        switch (sUriMatcher.match(uri)) {
            // If the incoming URI was for all of the table
            case URI_BO_TABLE_ID:

                if (TextUtils.isEmpty(sortOrder))
                	sortOrder = "_ID ASC";
                qb.setProjectionMap(sBuildOrderProjectionMap);
                break;

            // If the incoming URI was for a single row
            case URI_BO_ROW_ID:
                /*
                 * Because this URI was for a single row, the _ID value part is
                 * present. Get the last path segment from the URI; this is the _ID value.
                 * Then, append the value to the WHERE clause for the query
                 */
                selection = selection + "_ID = " + uri.getLastPathSegment();
                qb.setProjectionMap(sBuildOrderProjectionMap);
                break;

            default:
                // If the URI is not recognized, you should do some error handling here.
            	throw new IllegalArgumentException("URI not recognised: " + uri.toString());
        }
        
        String orderBy;
        // If no sort order is specified, uses the default
        if (TextUtils.isEmpty(sortOrder))
            orderBy = DbAdapter.TABLE_BUILD_ORDER_DEFAULT_SORT;
        else
            // otherwise, uses the incoming sort order
            orderBy = sortOrder;
        
        // Opens the database object in "read" mode, since no writes need to be done.
        SQLiteDatabase db = mDb.getWritableDatabase();

        /*
         * Performs the query. If no problems occur trying to read the database, then a Cursor
         * object is returned; otherwise, the cursor variable contains null. If no records were
         * selected, then the Cursor object is empty, and Cursor.getCount() returns 0.
         */
        Cursor c = qb.query(
            db,            // The database to query
            projection,    // The columns to return from the query
            selection,     // The columns for the where clause
            selectionArgs, // The values for the where clause
            null,          // don't group the rows
            null,          // don't filter by row groups
            orderBy        // The sort order
        );

        // Tells the Cursor what URI to watch, so it knows when its source data changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		return 0;
	}
}
