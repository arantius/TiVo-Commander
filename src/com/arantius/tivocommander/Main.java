package com.arantius.tivocommander;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class Main extends Activity {
  private static final String LOG_TAG = "tivo_main";

  protected Boolean checkSettings() {
    SharedPreferences prefs = PreferenceManager
        .getDefaultSharedPreferences(getBaseContext());
    String tivoAddr = prefs.getString("tivo_addr", "");
    String tivoMak = prefs.getString("tivo_mak", "");
    Integer tivoPort;
    try {
      tivoPort = Integer.parseInt(prefs.getString("tivo_port", ""));
    } catch (NumberFormatException e) {
      tivoPort = 0;
    }

    Log.i(LOG_TAG, "addr: " + tivoAddr);
    Log.i(LOG_TAG, "mak: " + tivoMak);
    Log.i(LOG_TAG, "port: " + tivoPort);

    if ("" == tivoAddr || "" == tivoMak || 0 >= tivoPort) {
      Intent i = new Intent(getBaseContext(), Settings.class);
      startActivity(i);
      return false;
    }

    return true;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (checkSettings()) {
      Intent i = new Intent(getBaseContext(), Catalog.class);
      startActivity(i);
      // Remove this (empty) activity from the stack.
      finish();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(LOG_TAG, ">>> onResume()");
    checkSettings();
  }
}
