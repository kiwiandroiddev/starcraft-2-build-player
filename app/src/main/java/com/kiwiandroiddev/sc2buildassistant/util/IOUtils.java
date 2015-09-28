package com.kiwiandroiddev.sc2buildassistant.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods relating to files on external storage (e.g. SD card)
 *
 * Created by matt on 17/07/15.
 */
public class IOUtils {
	public static void writeIntToSharedPrefs(Context c, String key, int value) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putInt(key, value).apply();
	}

	public static ArrayList<String> getAssetsWithExtension(Context c, String endsWith, String dir) throws IOException {
		String[] all_assets = c.getAssets().list(dir);
		ArrayList<String> results = new ArrayList<String>();
		for (String filename : all_assets) {
			if (filename.endsWith(endsWith))
				results.add(filename);
		}
		return results;
	}

	// credit: http://stackoverflow.com/questions/9530921/list-all-the-files-from-all-the-folder-in-a-single-list
	// recursive file search
	public static ArrayList<File> getFilesWithExtension(String endsWith, File parentDir) {
		ArrayList<File> inFiles = new ArrayList<File>();
		File[] files = parentDir.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				inFiles.addAll(getFilesWithExtension(endsWith, parentDir));
			} else {
				if (file.getName().endsWith(endsWith)){
					inFiles.add(file);
				}
			}
		}
		return inFiles;
	}

	public static List<String> getStringListFromAsset(Context c, String assetName) {
		// read asset file into string buffer
		String bufferString;
		try {
			InputStream input;
			input = c.getAssets().open(assetName);
			int size = input.available();
			byte[] buffer = new byte[size];
			input.read(buffer);
			input.close();
			bufferString = new String(buffer);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		Gson gson = new Gson();
		try {
			String[] list = gson.fromJson(bufferString, String[].class);
			return Arrays.asList(list);
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			return null;
		}
	}

//    public static void copyFile(InputStream in, OutputStream out) throws IOException {
//        byte[] buffer = new byte[1024];
//        int read;
//        while((read = in.read(buffer)) != -1){
//          out.write(buffer, 0, read);
//        }
//    }
}
