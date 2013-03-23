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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.arantius.tivocommander.Connect;
import com.arantius.tivocommander.Discover;
import com.arantius.tivocommander.R;
import com.arantius.tivocommander.Settings;
import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.request.BodyAuthenticate;
import com.arantius.tivocommander.rpc.request.CancelRpc;
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

  private static final String LOG_TAG = "tivo_commander";

  public static String mBodyId = "-";
  public static Boolean mBodyIsAuthed = false;
  public static String mTivoAddr;

  private static DataInputStream mInputStream;
  private static MindRpcInput mInputThread;
  private static Activity mOriginActivity;
  private static Bundle mOriginExtras;
  private static DataOutputStream mOutputStream;
  private static MindRpcOutput mOutputThread;
  private static TreeMap<Integer,MindRpcResponseListener> mResponseListenerMap =
      new TreeMap<Integer,MindRpcResponseListener>();
  private static volatile int mRpcId = 1;
  private static volatile int mSessionId;
  private static Socket mSocket;
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
    // Reconnect if necessary; but not for BodyAuthenticate!  That one RPC
    // is sent during connection as part of the verification.
    if (!isConnected() && !(request instanceof BodyAuthenticate)) {
      init2();
      return;
    }
    mOutputThread.addRequest(request);
    if (listener != null) {
      mResponseListenerMap.put(request.getRpcId(), listener);
    }
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

  private static boolean connect(final Activity originActivity) {
    Callable<Boolean> connectCallable = new Callable<Boolean>() {
      public Boolean call() {
        SSLSocketFactory sslSocketFactory = createSocketFactory(originActivity);
        if (sslSocketFactory == null) {
          return false;
        }

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

  private static SSLSocketFactory createSocketFactory(
      final Activity originActivity
  ) {
    final String password = readPassword(originActivity);
    try {
       KeyStore keyStore = KeyStore.getInstance("PKCS12");
       KeyManagerFactory fac = KeyManagerFactory.getInstance("X509");
       InputStream keyInput = originActivity.getResources().openRawResource(
           R.raw.cdata);

       keyStore.load(keyInput, password.toCharArray());
       keyInput.close();

       fac.init(keyStore, password.toCharArray());
       SSLContext context = SSLContext.getInstance("TLS");
       TrustManager[] tm = new TrustManager[] { new AlwaysTrustManager() };
       context.init(fac.getKeyManagers(), tm, new SecureRandom());
       return context.getSocketFactory();
    } catch (CertificateException e) {
      Log.e(LOG_TAG, "createSocketFactory: CertificateException!", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "createSocketFactory: IOException!", e);
    } catch (KeyManagementException e) {
      Log.e(LOG_TAG, "createSocketFactory: KeyManagementException!", e);
    } catch (KeyStoreException e) {
      Log.e(LOG_TAG, "createSocketFactory: KeyStoreException!", e);
    } catch (NoSuchAlgorithmException e) {
      Log.e(LOG_TAG, "createSocketFactory: NoSuchAlgorithmException!", e);
    } catch (UnrecoverableKeyException e) {
      Log.e(LOG_TAG, "createSocketFactory: UnrecoverableKeyException!", e);
    }
    return null;
  }

  /** Cancel all outstanding RPCs with response listeners. */
  public static void cancelAll() {
    for (Integer i : mResponseListenerMap.keySet()) {
      addRequest(new CancelRpc(i), null);
    }
    mResponseListenerMap.clear();
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
    mBodyIsAuthed = false;
    disconnectThread.start();
    try {
      disconnectThread.join();
    } catch (InterruptedException e) {
      Log.e(LOG_TAG, "disconnect() interrupted exception", e);
    }
  }

  protected static void dispatchResponse(final MindRpcResponse response) {
    final Integer rpcId = response.getRpcId();
    if (mResponseListenerMap.get(rpcId) == null) {
      return;
    }

    mOriginActivity.runOnUiThread(new Runnable() {
      public void run() {
        mResponseListenerMap.get(rpcId).onResponse(response);
        if (response.isFinal()) {
          mResponseListenerMap.remove(rpcId);
        }
      }
    });
  }

  public static Boolean getBodyIsAuthed() {
    return mBodyIsAuthed;
  }

  public static int getRpcId() {
    return mRpcId++;
  }

  public static int getSessionId() {
    return mSessionId;
  }

  /** Init step 1.  Every activity that's going to do RPCs should call this,
   * passing itself and its data bundle, if any. */
  public static boolean init(final Activity originActivity, Bundle originExtras) {
    // Always save these; they're used to resume later.
    mOriginExtras = originExtras;
    mOriginActivity = originActivity;

    if (isConnected()) {
      // Already connected?  No-op.
      Utils.log("MindRpc.init(): already connected.");
      return false;
    }

    Utils.log("MindRpc.init(); " + originActivity.toString());
    if (!checkSettings(originActivity)) {
      originActivity.finish();
      return true;
    }

    init2();
    return true;
  }

  /** Init continues here; it may resume here after disconnection. Fires off
   * the Connect activity to surface this flow to the user. */
  public static void init2() {
    Utils.log("MindRpc.init2(); " + mOriginActivity.toString());
    Intent intent =
        new Intent(mOriginActivity.getBaseContext(), Connect.class);
    mOriginActivity.startActivity(intent);
    mOriginActivity.finish();
  }

  /** Finally, (only) the Connect activity calls back here to finish init. */
  public static void init3(final Activity connectActivity) {
    Utils.log("MindRpc.init3() " + mOriginActivity.toString());

    stopThreads();
    disconnect();

    if (!connect(connectActivity)) {
      settingsError(mOriginActivity, R.string.error_connect);
      return;
    }

    mInputThread = new MindRpcInput(mInputStream);
    mInputThread.start();

    mOutputThread = new MindRpcOutput(mOutputStream);
    mOutputThread.start();

    MindRpcResponseListener authListener = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        if ("failure".equals(response.getBody().path("status").asText())) {
          settingsError(connectActivity, R.string.error_auth);
          connectActivity.finish();
        } else {
          mBodyIsAuthed = true;
          Intent intent =
              new Intent(connectActivity.getBaseContext(), mOriginActivity.getClass());
          if (mOriginExtras != null) {
            intent.putExtras(mOriginExtras);
          }
          mOriginExtras = null;
          connectActivity.startActivity(intent);
          connectActivity.finish();
          connectActivity.overridePendingTransition(0, 0);
        }
      }
    };
    Utils.log("MindRpc.init3(): start auth.");
    addRequest(new BodyAuthenticate(mTivoMak), authListener);
  }

  protected static boolean isConnected() {
    if (mInputThread == null
        || mInputThread.getState() == Thread.State.TERMINATED
        || mOutputThread == null
        || mOutputThread.getState() == Thread.State.TERMINATED) {
      mBodyIsAuthed = false;
      return false;
    }
    if (mSocket == null || mSocket.isClosed()) {
      mBodyIsAuthed = false;
      return false;
    }

    return mBodyIsAuthed;
  }

  private static String readPassword(Context ctx) {
    InputStream inputStream = ctx.getResources().openRawResource(
        R.raw.cdata_pass);
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(inputStream));
    try {
      return reader.readLine();
    } catch (IOException e) {
      Log.e(LOG_TAG, "readpassword: IOException!", e);
      return "";
    }
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

  public static void settingsError(final Activity activity, final int messageId) {
    Utils.log("Settings: " + activity.getResources().getString(messageId));
    activity.runOnUiThread(new Runnable() {
      public void run() {
        Toast
            .makeText(activity.getBaseContext(), messageId, Toast.LENGTH_SHORT)
            .show();
        Intent i;
        if (activity.getClass() == Discover.class) {
          i = new Intent(activity.getBaseContext(), Settings.class);
        } else {
          i = new Intent(activity.getBaseContext(), Discover.class);
        }
        activity.startActivity(i);
      }
    });
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
}
