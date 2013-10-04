/*
DVR Commander for TiVo allows control of a TiVo Premiere device.
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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.MulticastLock;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;

public class Discover extends ListActivity implements OnItemClickListener,
    ServiceListener, OnItemLongClickListener {
  private TextView mEmpty;
  private SimpleAdapter mHostAdapter;
  private volatile ArrayList<HashMap<String, Object>> mHosts =
      new ArrayList<HashMap<String, Object>>();
  private JmDNS mJmdns;
  private MulticastLock mMulticastLock = null;
  private final String mServiceNameRpc = "_tivo-mindrpc._tcp.local.";
  private final String mServiceNameVideos = "_tivo-videos._tcp.local.";
  private final String[] mServiceNames = new String[] {
      mServiceNameRpc, mServiceNameVideos };

  protected void addDeviceMap(final HashMap<String, Object> listItem) {
    final String addr = (String) listItem.get("addr");
    final String name = (String) listItem.get("name");
    final int warnIcon = (Integer) listItem.get("warn_icon");
    final int blank = R.drawable.blank;

    Integer oldIndex = null;
    for (HashMap<String, Object> host : mHosts) {
      if (name.equals(host.get("name")) && addr.equals(host.get("addr"))) {
        if ((Integer) host.get("warn_icon") != blank && warnIcon == blank) {
          oldIndex = mHosts.indexOf(host);
          listItem.put("deviceId", host.get("deviceId"));
          break;
        } else {
          Utils.log("Ignoring duplicate event.");
          return;
        }
      }
    }

    final Integer newIndex = oldIndex; // Final copy that the runnable can see.
    runOnUiThread(new Runnable() {
      public void run() {
        if (newIndex == null) {
          // We didn't detect an item above as an update, and we didn't short-
          // circuit to avoid duplicate events. So add a new item.
          mHosts.add(listItem);
        } else {
          mHosts.set(newIndex, listItem);
        }

        // And make it visible in the UI either way.
        mHostAdapter.notifyDataSetChanged();
      };
    });
  }

  public final void customDevice(View v) {
    editCustomDevice(new Device());
  }

  public final void editCustomDevice(final Device device) {
    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Custom Device");

    final LayoutInflater inflater =
        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    final View dialogView = inflater.inflate(R.layout.device_custom, null);
    builder.setView(dialogView);

    final EditText input_name = (EditText) dialogView.findViewById(
        R.id.input_name);
    input_name.setText(device.device_name);
    final EditText input_addr = (EditText) dialogView.findViewById(
        R.id.input_addr);
    input_addr.setText(device.addr);
    final EditText input_mak = (EditText) dialogView.findViewById(
        R.id.input_mak);
    input_mak.setText(device.mak);
    final EditText input_tsn = (EditText) dialogView.findViewById(
        R.id.input_tsn);
    input_tsn.setText(device.tsn);
    final EditText input_port = (EditText) dialogView.findViewById(
        R.id.input_port);
    input_port.setText(device.port.toString());

    final OnClickListener onClickListener =
        new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            device.device_name =
                input_name
                    .getText().toString();
            device.addr = ((EditText) dialogView.findViewById(R.id.input_addr))
                .getText().toString();
            device.mak = ((EditText) dialogView.findViewById(R.id.input_mak))
                .getText().toString();
            device.tsn = ((EditText) dialogView.findViewById(R.id.input_tsn))
                .getText().toString();
            if ("".equals(device.tsn))
              device.tsn = "-";
            final String portStr =
                ((EditText) dialogView.findViewById(R.id.input_port))
                    .getText().toString();
            try {
              device.port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
              device.port = 1413;
            }

            Database db = new Database(Discover.this);
            db.saveDevice(device);
            db.switchDevice(device);

            Intent intent = new Intent(Discover.this, NowShowing.class);
            startActivity(intent);
            Discover.this.finish();
          }
        };
    builder.setPositiveButton("OK", onClickListener);
    builder.setNegativeButton("Cancel", null);

    builder.create().show();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.disconnect();

    setTitle("TiVo Device Search");
    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list_discover);

    mEmpty = ((TextView) findViewById(android.R.id.empty));
    mHostAdapter =
        new SimpleAdapter(this, mHosts, R.layout.item_discover, new String[] {
            "name", "warn_icon" },
            new int[] { R.id.discover_name, R.id.discover_warn_icon });
    setListAdapter(mHostAdapter);

    final ListView lv = getListView();
    lv.setOnItemClickListener(this);
    lv.setOnItemLongClickListener(this);

    Utils.activateHomeButton(this);
  }

  public final boolean onCreateOptionsMenu(Menu menu) {
    Utils.createShortOptionsMenu(menu, this);
    return true;
  }

  public void onItemClick(AdapterView<?> parent, View view, int position,
      long id) {
    final HashMap<String, Object> item = mHosts.get(position);

    final Database db = new Database(Discover.this);
    final Long deviceId = (Long) item.get("deviceId");
    if (deviceId != null) {
      final Device clickedDevice = db.getDevice(deviceId);
      if (clickedDevice != null && !"".equals(clickedDevice.mak)) {
        db.switchDevice(clickedDevice);
        Intent intent = new Intent(Discover.this, NowShowing.class);
        startActivity(intent);
        this.finish();
        return;
      }
    }

    int messageId = (Integer) item.get("messageId");
    if (messageId > 0) {
      showHelp(messageId);
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("MAK");
    builder.setMessage(R.string.pref_mak_instructions);

    final EditText makEditText = new EditText(this);
    makEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
    builder.setView(makEditText);

    // TODO: Be DRY vs. the same in customDevice().
    final OnClickListener onClickListener =
        new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            Device device = db.getNamedDevice(
                (String) item.get("name"), (String) item.get("addr"));
            if (device == null) device = new Device();
            device.device_name = (String) item.get("name");
            device.addr = (String) item.get("addr");
            device.mak = makEditText.getText().toString();
            device.tsn = "-";
            try {
              final String portStr = (String) item.get("port");
              device.port = Integer.parseInt(portStr);
            } catch (ClassCastException e) {
              // I don't know why this is showing up, but handle it anyway.
              device.port = (Integer) item.get("port");
            }

            db.saveDevice(device);
            db.switchDevice(device);

            Intent intent = new Intent(Discover.this, NowShowing.class);
            startActivity(intent);
            Discover.this.finish();
          }
        };
    builder.setPositiveButton("OK", onClickListener);
    builder.setNegativeButton("Cancel", null);

    builder.create().show();
  }

  public boolean onItemLongClick(AdapterView<?> parent, View view,
      int position, long id) {
    final HashMap<String, Object> item = mHosts.get(position);
    final Long deviceId = (Long) item.get("deviceId");
    if (deviceId == null) {
      return false;
    }

    final Database db = new Database(this);
    final Device device = db.getDevice(deviceId);
    final ArrayList<String> choices = new ArrayList<String>();
    choices.add("Edit");
    choices.add("Delete");

    ArrayAdapter<String> choicesAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            choices);

    DialogInterface.OnClickListener onClickListener =
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int position) {
            switch (position) {
            case 0:
              editCustomDevice(device);
              break;
            case 1:
              db.deleteDevice(deviceId);
              stopQuery();
              startQuery(null);
              break;
            }
          }
        };

    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Operation?");
    dialogBuilder.setAdapter(choicesAdapter, onClickListener);
    dialogBuilder.create().show();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this, true);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Discover");
    stopQuery();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Discover");
    startQuery(null);
  }

  /** ServiceListener */
  public void serviceAdded(ServiceEvent event) {
    // Make sure serviceResolved() gets called.
    event.getDNS().requestServiceInfo(event.getType(), event.getName());
  }

  /** ServiceListener */
  public void serviceRemoved(ServiceEvent event) {
    // Ignore.
  }

  /** ServiceListener */
  public void serviceResolved(ServiceEvent event) {
    ServiceInfo info = event.getInfo();
    Utils.log("Discovery serviceResolved(): " + event.toString());
    if (mJmdns == null) {
      Utils.log("Ignoring because search is not running.");
      return;
    }

    addDevice(
        event.getName(),
        info.getHostAddresses()[0],
        Integer.toString(info.getPort()),
        info.getPropertyString("platform"),
        info.getType(),
        info.getPropertyString("TSN"));
  }

  // Separate so that I can pass mock testing data.
  void addDevice(
      final String name, final String addr, final String port,
      final String platform, final String type, final String tsn) {
    int messageId = 0;

    if (platform == null) {
      Utils.log("Unexpected: NULL platform.");
      messageId = R.string.premiere_only;
    } else if (platform.startsWith("pc/")
        || platform.equals("pc")
        || platform.startsWith("pyTivo")) {
      Utils.log("Ignoring event for PC platform.");
      // This is a e.g. a TiVo Desktop or pyTivo share. Exclude it.
      return;
    } else if (platform.startsWith("tcd/VM") || platform.startsWith("VM")) {
      messageId = R.string.no_virgin_media;
    } else if (platform.indexOf("Series4") == -1
        && platform.indexOf("Series5") == -1) {
      messageId = R.string.premiere_only;
    } else if (!mServiceNameRpc.equals(type)) {
      messageId = R.string.error_net_control;
    }

    final HashMap<String, Object> listItem = new HashMap<String, Object>();
    listItem.put("addr", addr);
    listItem.put("deviceId", null);
    listItem.put("messageId", messageId);
    listItem.put("name", name);
    listItem.put("port", port);
    listItem.put("tsn", tsn);
    listItem.put("warn_icon", messageId == 0 ? R.drawable.blank
        : android.R.drawable.ic_dialog_alert);
    addDeviceMap(listItem);
  }

  private final void showHelp(int messageId) {
    stopQuery();
    String message = getResources().getString(messageId);
    Utils.log("Showing help because:\n" + message);
    Intent intent = new Intent(Discover.this, Help.class);
    intent.putExtra("note", message);
    startActivity(intent);
  }

  public final void showHelp(View V) {
    stopQuery();
    Intent intent = new Intent(Discover.this, Help.class);
    startActivity(intent);
  }

  public final void startQuery(View v) {
    mHosts.clear();
    mHostAdapter.notifyDataSetChanged();

    // Add stored (i.e. custom) devices.
    final Database db = new Database(this);
    for (Device device : db.getDevices()) {
      final HashMap<String, Object> listItem = new HashMap<String, Object>();
      listItem.put("addr", device.addr);
      listItem.put("deviceId", device.id);
      listItem.put("messageId", -1);
      listItem.put("name", device.device_name);
      listItem.put("port", device.port.toString());
      listItem.put("tsn", "-");
      listItem.put("warn_icon", android.R.drawable.ic_menu_recent_history);
      addDeviceMap(listItem);
    }

    // Skip mDNS discovery on BlackBerry.
    final String osName = System.getProperty("os.name");
    Utils.log("Discover; os.name = " + osName);
    if ("qnx".equals(osName)) {
      if (mHosts.size() == 0) {
        Utils.toast(this, R.string.blackberry_discovery, Toast.LENGTH_LONG);
      }
      findViewById(R.id.refresh_button).setVisibility(View.GONE);
      return;
    }

    stopQuery();
    Utils.log("Start discovery query ...");
    mEmpty.setText("Searching ...");
    setProgressBarIndeterminateVisibility(true);
    findViewById(R.id.refresh_button).setEnabled(false);

    final Discover that = this;
    Thread jmdnsThread = new Thread(new Runnable() {
      public void run() {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        int intaddr = wifiInfo.getIpAddress();
        byte[] byteaddr =
            new byte[] { (byte) (intaddr & 0xff), (byte) (intaddr >> 8 & 0xff),
                (byte) (intaddr >> 16 & 0xff), (byte) (intaddr >> 24 & 0xff) };
        InetAddress addr;
        try {
          addr = InetAddress.getByAddress(byteaddr);
        } catch (UnknownHostException e1) {
          showHelp(R.string.error_get_wifi_addr);
          finish();
          return;
        }

        mMulticastLock =
            wifi.createMulticastLock("DVR Commander for TiVo Lock");
        mMulticastLock.setReferenceCounted(true);
        try {
          mMulticastLock.acquire();
        } catch (UnsupportedOperationException e) {
          showHelp(R.string.error_wifi_lock);
          finish();
          return;
        }

        try {
          mJmdns = JmDNS.create(addr, "localhost");
        } catch (IOException e1) {
          showHelp(R.string.error_multicast);
          finish();
          return;
        }

        for (String serviceName : mServiceNames) {
          mJmdns.addServiceListener(serviceName, that);
        }
      }
    });
    jmdnsThread.start();
    try {
      jmdnsThread.join();
    } catch (InterruptedException e1) {
      Utils.logError("jmdns thread interrupted", e1);
    }

    // Don't run for too long.
    new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(60000);
        } catch (InterruptedException e) {
          // Ignore.
        }
        stopQuery();
      }
    }).start();
  }

  protected final void stopQuery() {
    runOnUiThread(new Runnable() {
      public void run() {
        setProgressBarIndeterminateVisibility(false);
        View refreshButton = findViewById(R.id.refresh_button);
        if (refreshButton != null) {
          refreshButton.setEnabled(true);
        }
        if (mEmpty != null) {
          mEmpty.setText("No results found.");
        }
      }
    });

    // JmDNS close seems to take ~6 seconds, so do that on a background thread.
    if (mJmdns != null) {
      Utils.log("Stop discovery query ...");
      final JmDNS oldMdns = mJmdns;
      mJmdns = null;
      new Thread(new Runnable() {
        public void run() {
          try {
            for (String serviceName : mServiceNames) {
              oldMdns.removeServiceListener(serviceName, Discover.this);
            }
            oldMdns.close();
          } catch (RuntimeException e) {
            Utils.logError("Could not close JmDNS!", e);
          } catch (IOException e) {
            Utils.logError("Could not close JmDNS!", e);
          }
        }
      }).start();
    }

    if (mMulticastLock != null) {
      try {
        mMulticastLock.release();
      } catch (RuntimeException e) {
        // Ignore. Likely
        // "MulticastLock under-locked DVR Commander for TiVo Lock".
      }
      mMulticastLock = null;
    }
  }
}
