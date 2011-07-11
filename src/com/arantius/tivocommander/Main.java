package com.arantius.tivocommander;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;

public class Main extends Activity {
  private static final String LOG_TAG = "tivo_main";

  public static volatile String mTivoAddr;
  public static volatile int mTivoPort;
  public static volatile String mTivoMak;

  private boolean checkSettings() {
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    mTivoAddr = prefs.getString("tivo_addr", "");
    try {
      mTivoPort = Integer.parseInt(prefs.getString("tivo_port", ""));
    } catch (NumberFormatException e) {
      mTivoPort = 0;
    }
    mTivoMak = prefs.getString("tivo_mak", "");

    int error = 0;
    if ("" == mTivoAddr) {
      error = R.string.error_addr;
    } else if (0 >= mTivoPort) {
      error = R.string.error_port;
    } else if ("" == mTivoMak) {
      error = R.string.error_mak;
    }

    if (error != 0) {
      settingsError(error);
      return false;
    }

    return true;
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.i(LOG_TAG, ">>> onResume()");
    if (checkSettings()) {
      startRpc();
      // Start the catalog activity ...
      Intent i = new Intent(getBaseContext(), Catalog.class);
      startActivity(i);
      // ... and remove this (empty) activity from the stack.
      finish();
    }
    Log.i(LOG_TAG, "<<< onResume()");
  }

  public void settingsError(int messageId) {
    Toast.makeText(getApplicationContext(), messageId, Toast.LENGTH_SHORT)
        .show();
    Intent i = new Intent(getBaseContext(), Settings.class);
    startActivity(i);
  }

  private void startRpc() {
    Log.i(LOG_TAG, ">>> startRpc()");
    if (checkSettings()) {
      int errorCode = MindRpc.init(this);
      if (errorCode != 0) {
        settingsError(errorCode);
      }
    }
  }
}
