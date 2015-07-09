package org.citruscircuits.scout;

import java.io.File;
import java.util.Arrays;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

public class StoredMatchesAdapter extends ArrayAdapter<String> {
	public StoredMatchesAdapter(Context context, int resource) {
		super(context, resource);
	}
	
	public void loadDir(File dir) {
		if (!dir.exists()) {
			dir.mkdirs();
		}
		String[] files = dir.list();
		Arrays.sort(files, new AlphanumComparator());
		
		clear();
		for(String file : files) {
			add(file);
		}
		Log.e("rawr", files.toString());
	}
}
