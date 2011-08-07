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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class Discover extends ListActivity {
  protected final class AddHost implements Runnable {
    private final String mAddr;
    private final String mName;
    private final int mPort;

    public AddHost(String name, String addr, int port) {
      mAddr = addr;
      mName = name;
      mPort = port;
    }

    public void run() {
      HashMap<String, String> listItem = new HashMap<String, String>();
      listItem.put("addr", mAddr);
      listItem.put("name", mName);
      listItem.put("port", new Integer(mPort).toString());
      mHosts.add(listItem);
      mHostAdapter.notifyDataSetChanged();
    }
  }

  private MulticastLock mMulticastLock = null;
  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          if (position >= 0) {
            HashMap<String, String> item = mHosts.get(position);
            SharedPreferences prefs =
                PreferenceManager.getDefaultSharedPreferences(Discover.this
                    .getBaseContext());
            Editor editor = prefs.edit();
            editor.putString("tivo_addr", item.get("addr"));
            editor.putString("tivo_port", item.get("port"));
            editor.commit();

            Toast.makeText(getApplicationContext(), "Ok!  Just set the MAK.",
                Toast.LENGTH_SHORT).show();
          }

          Intent intent = new Intent(Discover.this, Settings.class);
          startActivity(intent);
          finish();
        }
      };
  private final ServiceListener mServiceListener = new ServiceListener() {
    public void serviceAdded(ServiceEvent event) {
      // Required to force serviceResolved to be called again
      // (after the first search)
      event.getDNS().requestServiceInfo(event.getType(), event.getName(),
          mTimeout);
    }

    public void serviceRemoved(ServiceEvent event) {
    }

    public void serviceResolved(ServiceEvent event) {
      ServiceInfo info = event.getInfo();
      runOnUiThread(new AddHost(event.getName(), info.getHostAddresses()[0],
          info.getPort()));
    }
  };

  protected final Activity mActivity = this;
  protected SimpleAdapter mHostAdapter;
  protected ArrayList<HashMap<String, String>> mHosts =
      new ArrayList<HashMap<String, String>>();
  protected JmDNS mJmdns;
  protected final String mServiceName = "_tivo-mindrpc._tcp.local.";
  protected final long mTimeout = 2500;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list);

    mHosts.clear();
    mHostAdapter =
        new SimpleAdapter(this, mHosts, android.R.layout.simple_list_item_1,
            new String[] { "name" }, new int[] { android.R.id.text1 });
    setListAdapter(mHostAdapter);
    runOnUiThread(new AddHost("Custom settings", null, 0));

    getListView().setOnItemClickListener(mOnClickListener);
  }

  @Override
  protected void onResume() {
    super.onResume();

    android.net.wifi.WifiManager wifi =
        (android.net.wifi.WifiManager) getSystemService(android.content.Context.WIFI_SERVICE);
    mMulticastLock = wifi.createMulticastLock("HeeereDnssdLock");
    mMulticastLock.setReferenceCounted(false);
    mMulticastLock.acquire();

    setProgressBarIndeterminateVisibility(true);
    new Thread(new Runnable() {
      public void run() {
        try {
          mJmdns = JmDNS.create();
        } catch (IOException e) {
          Utils.logError("Couldn't do mDNS", e);
          return;
        }
        mJmdns.addServiceListener(mServiceName, mServiceListener);
        mJmdns.requestServiceInfo(mServiceName, "", mTimeout);
        stopQuery();
        runOnUiThread(new Runnable() {
          public void run() {
            setProgressBarIndeterminateVisibility(false);
          }
        });
      }
    }).start();
  }

  @Override
  protected void onStop() {
    super.onStop();
    stopQuery();
  }

  protected final void stopQuery() {
    if (mJmdns != null) {
      mJmdns.removeServiceListener(mServiceName, mServiceListener);
      try {
        mJmdns.close();
      } catch (IOException e) {
        Utils.logError("Closing jmdns", e);
      }
      mJmdns = null;
    }

    if (mMulticastLock != null) {
      mMulticastLock.release();
      mMulticastLock = null;
    }
  }
}
