package com.arantius.tivocommander;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;

public class Catalog extends ListActivity {
  private static final String[] mFeatures = { "Remote", "My Shows", "Search",
      "Settings" };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final ListAdapter adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
            mFeatures);
    setListAdapter(adapter);

    final ListView lv = getListView();
    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        Intent intent = null;
        switch (position) {
        case 0:
          intent = new Intent(getBaseContext(), Remote.class);
          break;
        case 1:
          intent = new Intent(getBaseContext(), MyShows.class);
          break;
        case 2:
          intent = new Intent(getBaseContext(), Search.class);
          break;
        case 3:
          intent = new Intent(getBaseContext(), Settings.class);
          break;
        default:
          Toast.makeText(getApplicationContext(), "Not Implemented",
              Toast.LENGTH_SHORT).show();
        }

        if (intent != null) {
          startActivity(intent);
        }
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
