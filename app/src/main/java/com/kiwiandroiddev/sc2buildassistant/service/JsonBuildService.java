package com.kiwiandroiddev.sc2buildassistant.service;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.kiwiandroiddev.sc2buildassistant.BuildOrderProvider;
import com.kiwiandroiddev.sc2buildassistant.MyApplication;
import com.kiwiandroiddev.sc2buildassistant.adapter.DbAdapter;
import com.kiwiandroiddev.sc2buildassistant.model.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import timber.log.Timber;

/**
 * Contains static functions for writing and reading {@link Build} objects to/from JSON files.
 *
 * Created by matt on 17/07/15.
 */
public class JsonBuildService {

    public static final String BUILDS_DIR = "sc2_builds";

    /**
     * Helper to write a build object as a JSON file to the builds directory on external storage
     *
     * @param filename output filename without parent directory (e.g. "6pool.json")
     * @param build Build object to serialize
     */
    public static void writeBuildToJsonFile(String filename, Build build) throws IOException, FileNotFoundException {
        // use GSON to serialize it to a JSON string
        ArrayList<Build> list = new ArrayList<Build>();
        list.add(build);

        Gson gson = new GsonBuilder()
            .setDateFormat(DbAdapter.DATE_FORMAT.toPattern())	// use ISO-8601 date format
            .create();

        final String json = gson.toJson(list);

        // write JSON file to file system
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, BUILDS_DIR + "/" + filename);

        file.createNewFile();
        OutputStream out = new FileOutputStream(file);
        out.write(json.getBytes());
        out.flush();
        out.close();
    }

    /**
     * Parses a JSON file, returning a list of Build objects.
     */
    public static ArrayList<Build> readBuildsFromJsonFile(File json_file) throws IOException, JsonSyntaxException {
        return readBuildsFromJsonInputStream(new FileInputStream(json_file));
    }

    /**
     * Parses a JSON input stream, returning a list of Build objects.
     */
    public static ArrayList<Build> readBuildsFromJsonInputStream(InputStream input) throws IOException, JsonSyntaxException {
        Gson gson = new GsonBuilder()
                .setDateFormat(DbAdapter.DATE_FORMAT.toPattern())	// use ISO-8601 date format
                .create();
        String bufferString = "";
        int size = input.available();
        byte[] buffer = new byte[size];
        input.read(buffer);
        input.close();
        bufferString = new String(buffer);

        Build[] builds = gson.fromJson(bufferString, Build[].class);
        ArrayList<Build> result = new ArrayList<Build>(Arrays.asList(builds));
        return result;
    }

    /**
     * Helper for reading in Build object(s) from a JSON file, then attempting to
     * load it/them into the database
     *
     * @param filename
     */
    public static void importBuildsFromJsonFileToDatabase(Context c, String filename) {
    	DbAdapter db = ((MyApplication) c.getApplicationContext()).getDb();
    	ArrayList<Build> newBuilds;
    	File file = new File(filename);

    	try {
    		newBuilds = readBuildsFromJsonFile(file);
    	} catch (JsonSyntaxException e) {
			Timber.e("JSON syntax error with file " + file);
            Toast.makeText(c, "Couldn't load " + file.toString() + ", invalid JSON syntax", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
		} catch (IOException e) {
			// TODO make this more informative
			Timber.e("IO error with file " + file);
            Toast.makeText(c, "Couldn't load " + file.toString() + ", input/ouput error", Toast.LENGTH_LONG).show();
            e.printStackTrace();
            return;
		}

//    	Timber.d(TAG, "newBuilds = " + newBuilds);
    	for (Build build : newBuilds) {
    		try { db.addBuild(build); }
    		catch (DbAdapter.NameNotUniqueException e) {
    			// TODO: move to strings.xml
    			Toast.makeText(c, "Couldn't import \"" + build.getName() + "\" as there is another build with that name. Please delete the old one first.", Toast.LENGTH_LONG).show();
    		}
    	}
    	notifyBuildProviderObservers(c);
    }

    /**
     * Creates builds directory on user's SD card if needed
     *
     * @return false if the builds dir doesn't exist and couldn't be created,
     * true otherwise
     */
    public static boolean createBuildsDirectory() {
        File root = Environment.getExternalStorageDirectory();
        File buildsDir = new File(root, BUILDS_DIR);
        if (!buildsDir.exists()) {
            return buildsDir.mkdirs();
        }
        return true;
    }

    /**
     * Notify observers of buildprovider's build table that its contents have changed
     */
    public static void notifyBuildProviderObservers(Context c) {
		Uri buildTableUri = Uri.withAppendedPath(BuildOrderProvider.BASE_URI, DbAdapter.TABLE_BUILD_ORDER);
		c.getContentResolver().notifyChange(buildTableUri, null);
    }
}
