package com.arantius.tivocommander.rpc;

import android.util.Log;

import com.arantius.tivocommander.Main;

public class MindRpc extends Thread {
  private static final String LOG_TAG = "tivo_mindrpc";

  // private final String mTivoAddr;
  // private final Integer mTivoPort;
  // private final String mTivoMak;
  //
  // public MindRpc(String addr, Integer port, String mak) {
  // mTivoAddr = addr;
  // mTivoPort = port;
  // mTivoMak = mak;
  // }

  public void connect() {
    Log.i(LOG_TAG, ">>> connect() ...");
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, ">>> MindRPC run() ...");

    // Connect here.
    Log.i(LOG_TAG, "addr: " + Main.mTivoAddr);
    Log.i(LOG_TAG, "port: " + Main.mTivoPort);
    Log.i(LOG_TAG, "mak: " + Main.mTivoMak);

    while (true) {
      // Limit worst case battery consumption?
      try {
        Thread.sleep(333);
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "MindRPC sleep was interrupted!");
      }

      // Do network I/O here.
      Log.i(LOG_TAG, "This should be the network loop ...");
    }
  }
}
