package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Catalog extends ListActivity {
  private static final String LOG_TAG = "tivo_catalog";

  private HashMap<String, String> listItem(int id) {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("id", String.valueOf(id));
    map.put("name", this.getString(id));
    return map;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ArrayList<HashMap<String, String>> listItems = new ArrayList<HashMap<String, String>>();
    listItems.add(this.listItem(R.string.catalog_remote));
    listItems.add(this.listItem(R.string.catalog_my_shows));
    listItems.add(this.listItem(R.string.catalog_browse));
    listItems.add(this.listItem(R.string.catalog_guide));
    listItems.add(this.listItem(R.string.catalog_settings));

    final ListAdapter adapter = new SimpleAdapter(this, listItems,
        R.layout.list_item, new String[] { "name" },
        new int[] { R.id.catalog_name });
    setListAdapter(adapter);

    final ListView lv = getListView();
    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        @SuppressWarnings("unchecked")
        HashMap<String, String> map = (HashMap<String, String>) adapter
            .getItem(position);

        int listItemId = Integer.parseInt(map.get("id"));
        switch (listItemId) {
        case R.string.catalog_settings:
          Intent i = new Intent(getBaseContext(), Settings.class);
          startActivity(i);
          break;
        default:
          Toast.makeText(getApplicationContext(), "Not Implemented",
              Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  @Override
  public void onDestroy() {
    super.onPause();
    Log.i(LOG_TAG, ">>> onDestroy()");
  }

  @Override
  public void onPause() {
    super.onPause();
    Log.i(LOG_TAG, ">>> onPause()");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(LOG_TAG, ">>> onResume()");
  }

  @Override
  public void onStop() {
    super.onPause();
    Log.i(LOG_TAG, ">>> onStop()");
  }
}
