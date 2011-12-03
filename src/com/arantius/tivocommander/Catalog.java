/*
TiVo Commander allows control of a TiVo Premiere device.
Copyright (C) 2011  Anthony Lieuallen (arantius@gmail.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;

public class Catalog extends ListActivity {
  private final OnItemClickListener mOnItemClickListener =
      new OnItemClickListener() {
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
            intent = new Intent(getBaseContext(), Discover.class);
            break;
          case 4:
            intent = new Intent(getBaseContext(), Help.class);
            break;
          case 5:
            intent = new Intent(getBaseContext(), About.class);
            break;
          default:
            Toast.makeText(getApplicationContext(), "Not Implemented",
                Toast.LENGTH_SHORT).show();
          }

          if (intent != null) {
            startActivity(intent);
          }
        }
      };

  private HashMap<String, Object> listItem(String title, Integer icon) {
    HashMap<String, Object> listItem = new HashMap<String, Object>();
    listItem.put("title", title);
    listItem.put("icon", icon);
    return listItem;
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ArrayList<HashMap<String, Object>> listItems =
        new ArrayList<HashMap<String, Object>>();
    // TODO: Manage
    // TODO: Now Playing
    listItems.add(listItem("Remote", R.drawable.icon_remote));
    listItems.add(listItem("My Shows", R.drawable.icon_tv32));
    listItems.add(listItem("Search", R.drawable.icon_search));
    listItems.add(listItem("Settings", R.drawable.icon_cog));
    listItems.add(listItem("Help", R.drawable.icon_help));
    listItems.add(listItem("About", R.drawable.icon_info));

    final ListAdapter adapter =
        new SimpleAdapter(this, listItems, R.layout.item_catalog, new String[] {
            "icon", "title" }, new int[] { R.id.icon, R.id.title });
    setListAdapter(adapter);

    final ListView lv = getListView();
    lv.setOnItemClickListener(mOnItemClickListener);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Catalog");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Catalog");
    MindRpc.init(this);
  }
}
