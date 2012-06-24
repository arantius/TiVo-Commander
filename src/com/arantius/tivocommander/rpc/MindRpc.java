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

package com.arantius.tivocommander.rpc;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.arantius.tivocommander.Discover;
import com.arantius.tivocommander.R;
import com.arantius.tivocommander.Settings;
import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.request.BodyAuthenticate;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public enum MindRpc {
  INSTANCE;

  private static class AlwaysTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] cert, String authType)
        throws CertificateException {
    }

    public void checkServerTrusted(X509Certificate[] cert, String authType)
        throws CertificateException {
    }

    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }
  }

  public static String mBodyId = "-";

  private static final String LOG_TAG = "tivo_commander";
  private static DataInputStream mInputStream;
  private static MindRpcInput mInputThread;
  private static Activity mOriginActivity;
  private static DataOutputStream mOutputStream;
  private static MindRpcOutput mOutputThread;
  private static HashMap<Integer, MindRpcResponseListener> mResponseListenerMap =
      new HashMap<Integer, MindRpcResponseListener>();
  private static volatile int mRpcId = 1;
  private static volatile int mSessionId;
  private static Socket mSocket;
  private static String mTivoAddr;
  private static String mTivoMak;
  private static int mTivoPort;
  private static final int TIMEOUT_CONNECT = 2500;

  /**
   * Add an outgoing request to the queue.
   *
   * @param request The request to be sent.
   * @param listener The object to notify when the response(s) come back.
   */
  public static void addRequest(MindRpcRequest request,
      MindRpcResponseListener listener) {
    if (mOutputThread == null) {
      if (mOriginActivity == null) {
        Utils.log("Tried to add a request while mOutputThread is null!  "
            + "mOriginActivity is null too!  I'm about to die ...");
      } else {
        Utils.log("Tried to add a request while mOutputThread is null!  "
            + "Doing .init() ...");
        MindRpc.init(mOriginActivity);
      }
    }
    mOutputThread.addRequest(request);
    if (listener != null) {
      mResponseListenerMap.put(request.getRpcId(), listener);
    }
  }

  public static void disconnect() {
    Thread disconnectThread = new Thread(new Runnable() {
      public void run() {
        // TODO: Do disconnect on close (after N idle seconds?).
        stopThreads();
        if (mSocket != null) {
          try {
            mSocket.close();
          } catch (IOException e) {
            Log.e(LOG_TAG, "disconnect() socket", e);
          }
        }
        if (mInputStream != null) {
          try {
            mInputStream.close();
          } catch (IOException e) {
            Log.e(LOG_TAG, "disconnect() input stream", e);
          }
        }
        if (mOutputStream != null) {
          try {
            mOutputStream.close();
          } catch (IOException e) {
            Log.e(LOG_TAG, "disconnect() output stream", e);
          }
        }
      }
    });
    disconnectThread.start();
    try {
      disconnectThread.join();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "disconnect() interrupted exception", e);
    }
  }

  public static int getRpcId() {
    return mRpcId++;
  }

  public static int getSessionId() {
    return mSessionId;
  }

  public static void init(final Activity originActivity) {
    mOriginActivity = originActivity;

    if (checkConnected()) {
      // Already connected? Great.
      return;
    }

    stopThreads();
    disconnect();

    if (!checkSettings(originActivity)) {
      return;
    }

    if (!connect()) {
      settingsError(originActivity, R.string.error_connect);
      return;
    }

    mInputThread = new MindRpcInput(mInputStream);
    mInputThread.start();

    mOutputThread = new MindRpcOutput(mOutputStream);
    mOutputThread.start();

    addRequest(new BodyAuthenticate(mTivoMak), new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        if ("failure".equals(response.getBody().path("status").getTextValue())) {
          settingsError(originActivity, R.string.error_auth);
        }
      }
    });
  }

  public static void saveBodyId(String bodyId) {
    if (bodyId == null || bodyId == "" || bodyId == mBodyId) {
      return;
    }

    mBodyId = bodyId;
    SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(mOriginActivity
            .getBaseContext());
    Editor edit = prefs.edit();
    edit.putString("tivo_tsn", bodyId);
    edit.commit();
  }

  public static void settingsError(Activity activity, int messageId) {
    Utils.log("Settings: " + activity.getResources().getString(messageId));
    Toast.makeText(activity.getBaseContext(), messageId, Toast.LENGTH_SHORT)
        .show();
    Intent i;
    if (activity.getClass() == Discover.class) {
      i = new Intent(activity.getBaseContext(), Settings.class);
    } else {
      i = new Intent(activity.getBaseContext(), Discover.class);
    }
    activity.startActivity(i);
  }

  private static boolean checkConnected() {
    if (mInputThread == null
        || mInputThread.getState() == Thread.State.TERMINATED
        || mOutputThread == null
        || mOutputThread.getState() == Thread.State.TERMINATED) {
      return false;
    }
    if (mSocket == null || mSocket.isClosed()) {
      return false;
    }

    return true;
  }

  private static boolean checkSettings(Activity activity) {
    SharedPreferences prefs =
        PreferenceManager
            .getDefaultSharedPreferences(activity.getBaseContext());

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
      settingsError(activity, error);
      return false;
    }

    // No errors found, so load the bodyId value.
    mBodyId = prefs.getString("tivo_tsn", "-");

    return true;
  }

  private static boolean connect() {
    Callable<Boolean> connectCallable = new Callable<Boolean>() {
      public Boolean call() throws Exception {
        SSLSocketFactory sslSocketFactory = null;

        // Set up the socket factory.
        try {
          TrustManager[] tm = new TrustManager[] { new AlwaysTrustManager() };
          SSLContext context = SSLContext.getInstance("TLS");
          context.init(new KeyManager[0], tm, new SecureRandom());

          sslSocketFactory = context.getSocketFactory();
        } catch (KeyManagementException e) {
          Log.e(LOG_TAG, "ssl: KeyManagementException!", e);
          return false;
        } catch (NoSuchAlgorithmException e) {
          Log.e(LOG_TAG, "ssl: NoSuchAlgorithmException!", e);
          return false;
        }

        // And use it to create a socket.
        try {
          mSessionId = 0x26c000 + new Random().nextInt(0xFFFF);
          mSocket = sslSocketFactory.createSocket();
          InetSocketAddress remoteAddr =
              new InetSocketAddress(mTivoAddr, mTivoPort);
          mSocket.connect(remoteAddr, TIMEOUT_CONNECT);
          mInputStream = new DataInputStream(mSocket.getInputStream());
          mOutputStream = new DataOutputStream(mSocket.getOutputStream());
        } catch (UnknownHostException e) {
          Log.e(LOG_TAG, "connect: unknown host!", e);
          return false;
        } catch (IOException e) {
          Log.e(LOG_TAG, "connect: io exception!", e);
          return false;
        }

        return true;
      }
    };

    ExecutorService executor = new ScheduledThreadPoolExecutor(1);
    Future<Boolean> success = executor.submit(connectCallable);
    try {
      return success.get();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "connect: interrupted exception!", e);
      return false;
    } catch (ExecutionException e) {
      Log.e(LOG_TAG, "connect: execution exception!", e);
      return false;
    }
  }

  private static void stopThreads() {
    if (mInputThread != null) {
      mInputThread.mStopFlag = true;
      mInputThread.interrupt();
      mInputThread = null;
    }
    if (mOutputThread != null) {
      mOutputThread.mStopFlag = true;
      mOutputThread.interrupt();
      mOutputThread = null;
    }
  }

  protected static void dispatchResponse(final MindRpcResponse response) {
    final Integer rpcId = response.getRpcId();
    if (!mResponseListenerMap.containsKey(rpcId)) {
      return;
    }

    mOriginActivity.runOnUiThread(new Runnable() {
      public void run() {
        mResponseListenerMap.get(rpcId).onResponse(response);
        // TODO: Remove only when the response .isFinal().
        mResponseListenerMap.remove(rpcId);
      }
    });
  }
}
