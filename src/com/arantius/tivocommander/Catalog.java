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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
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
  private class CustomExceptionHandler implements UncaughtExceptionHandler {
    private final UncaughtExceptionHandler defaultHandler;

    public CustomExceptionHandler() {
      defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public void uncaughtException(Thread thread, Throwable ex) {
      Utils.logError("Unhandled exception", ex);
      FileOutputStream fos;
      try {
        fos = Catalog.this.openFileOutput(CRASH_LOG, Context.MODE_PRIVATE);
      } catch (FileNotFoundException e) {
        defaultHandler.uncaughtException(thread, ex);
        return;
      }
      try {
        fos.write(Utils.getLog().getBytes());
        fos.close();
      } catch (IOException e) {
        defaultHandler.uncaughtException(thread, ex);
        return;
      }

      defaultHandler.uncaughtException(thread, ex);
    }
  }

  private final static String CRASH_LOG = "crash-log.txt";
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
            intent = new Intent(getBaseContext(), About.class);
            break;
          case 5:
            intent = new Intent(getBaseContext(), ProblemReport.class);
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

  private final void checkCrashLog() {
    FileInputStream fis;
    try {
      fis = openFileInput(CRASH_LOG);
    } catch (FileNotFoundException e) {
      // No log is good, ignore!
      return;
    }

    byte[] crashLogBytes;
    try {
      crashLogBytes = new byte[fis.available()];
      fis.read(crashLogBytes);
    } catch (IOException e) {
      Utils.logError("Reading crash log", e);
      try {
        fis.close();
      } catch (IOException e1) {
        Utils.logError("Closing crash log", e);
      }
      return;
    }
    final String crashLog = new String(crashLogBytes);
    deleteFile(CRASH_LOG);

    new AlertDialog.Builder(Catalog.this)
        .setTitle("Whoops!")
        .setMessage(
            "Looks like I crashed last time.  Send crash report to developer?")
        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int whichButton) {
            Utils.mailLog(crashLog, Catalog.this, "Crash Report");
          }
        }).setNegativeButton("No", null).create().show();
  }

  private HashMap<String, Object> listItem(String title, Integer icon) {
    HashMap<String, Object> listItem = new HashMap<String, Object>();
    listItem.put("title", title);
    listItem.put("icon", icon);
    return listItem;
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    checkCrashLog();
    Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler());

    ArrayList<HashMap<String, Object>> listItems =
        new ArrayList<HashMap<String, Object>>();
    // TODO: Manage
    // TODO: Now Playing
    listItems.add(listItem("Remote", R.drawable.icon_remote));
    listItems.add(listItem("My Shows", R.drawable.icon_tv32));
    listItems.add(listItem("Search", R.drawable.icon_search));
    listItems.add(listItem("Settings", R.drawable.icon_cog));
    listItems.add(listItem("About", R.drawable.icon_info));
//    listItems.add(listItem("Problem Report", R.drawable.icon_bug));

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
