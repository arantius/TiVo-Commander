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

import java.io.File;
import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.view.View;

import com.arantius.tivocommander.rpc.MindRpc;

public class Connect extends Activity {
  private static Thread mConnectThread;
  private static Thread mLimitThread;
  private static Thread mShowCancelThread;

  public void doCancel(View v) {
    stopThreads();
    Intent intent = new Intent(getBaseContext(), Discover.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(intent);
    finish();
  }

  private void stopThreads() {
    MindRpc.mConnectInterrupted = true;
    if (mConnectThread != null) mConnectThread.interrupt();
    if (mLimitThread != null) mLimitThread.interrupt();
    if (mShowCancelThread != null) mShowCancelThread.interrupt();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Utils.log("Activity:Create:Connect");
    setContentView(R.layout.connect);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Connect");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Connect");

    // However we're connecting, now is a good time to (re-)start caching.
    HttpResponseCache cache = HttpResponseCache.getInstalled();
    if (cache != null) {
      cache.flush();
    }
    try {
      File httpCacheDir = new File(this.getCacheDir(), "http");
      long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
      HttpResponseCache.install(httpCacheDir, httpCacheSize);
    } catch (IOException e) {
      Utils.logError("HTTP response cache installation failed:", e);
    }

    // Start on a separate thread so this UI shows immediately.
    mConnectThread = new Thread(new Runnable() {
      public void run() {
        MindRpc.init3(Connect.this);
        stopThreads();
      }
    });

    // A second thread limits the infinite loop built in to the above.
    mLimitThread = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(30000);
        } catch (InterruptedException e) {
          return;
        }

        // We were not interrupted, so we ran too long.  Error.
        doCancel(null);
        Connect.this.overridePendingTransition(0, 0);
      }
    });

    // While a third allows the user to cancel even sooner.
    mShowCancelThread = new Thread(new Runnable() {
      public void run() {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          return;
        }

        runOnUiThread(new Runnable() {
          public void run() {

            View b = findViewById(R.id.cancel);
            if (b != null) {
              b.setVisibility(View.VISIBLE);
            }
          }
        });
      }
    });

    mConnectThread.start();
    mLimitThread.start();
    mShowCancelThread.start();
  }
}
