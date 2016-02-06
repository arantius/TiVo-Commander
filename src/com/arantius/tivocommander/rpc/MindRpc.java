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
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Locale;
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.arantius.tivocommander.Connect;
import com.arantius.tivocommander.Database;
import com.arantius.tivocommander.Device;
import com.arantius.tivocommander.Discover;
import com.arantius.tivocommander.NowShowing;
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

  public static Boolean mBodyIsAuthed = false;
  public static Device mTivoDevice;

  private static DataInputStream mInputStream;
  private static MindRpcInput mInputThread;
  private static Activity mOriginActivity;
  private static Bundle mOriginExtras;
  private static DataOutputStream mOutputStream;
  private static MindRpcOutput mOutputThread;
  private static TreeMap<Integer, MindRpcResponseListener> mResponseListenerMap =
      new TreeMap<Integer, MindRpcResponseListener>();
  private static volatile int mRpcId = 1;
  private static volatile int mSessionId;
  private static Socket mSocket;
  private static final int TIMEOUT_CONNECT = 25000;

  /**
   * Add an outgoing request to the queue.
   *
   * @param request The request to be sent.
   * @param listener The object to notify when the response(s) come back.
   */
  public static void addRequest(MindRpcRequest request,
      MindRpcResponseListener listener) {
    // Reconnect if necessary; but not for BodyAuthenticate! That one RPC
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
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
        activity.getBaseContext());

    Utils.DEBUG_LOG = prefs.getBoolean("debug_log", false);

    final Database db = new Database(activity);
    db.portLegacySettings(activity);
    mTivoDevice = db.getLastDevice();

    int error = 0;
    if (mTivoDevice == null) {
      error = R.string.error_no_device;
    } else if (mTivoDevice.addr == null || "".equals(mTivoDevice.addr)) {
      error = R.string.error_addr;
    } else if (mTivoDevice.port == null || 0 >= mTivoDevice.port) {
      error = R.string.error_port;
    } else if (mTivoDevice.mak == null || "".equals(mTivoDevice.mak)) {
      error = R.string.error_mak;
    }

    if (error != 0) {
      settingsError(activity, error);
      return false;
    }

    return true;
  }

  private static boolean connect(final Activity originActivity) {
    Callable<Boolean> connectCallable = new Callable<Boolean>() {
      public Boolean call() {
        Utils.log(String.format(Locale.US,
            "Connecting to %s:%d ...", mTivoDevice.addr, mTivoDevice.port
            ));

        Utils.log(">>> With interfaces:");
        Enumeration<NetworkInterface> ifaces = null;
        try {
          ifaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
          Utils.log("Cannot get interfaces!");
        }
        while (ifaces != null && ifaces.hasMoreElements()) {
          NetworkInterface iface = ifaces.nextElement();
          StringBuilder ifaceStrBld = new StringBuilder();
          ifaceStrBld.append(String.format(Locale.US,
              "    %s %s",
              iface.getName(), iface.getDisplayName()
              ));

          boolean haveAddr = false;
          for (InterfaceAddress addr : iface.getInterfaceAddresses()) {
            if (addr.getAddress().isLoopbackAddress()) continue;
            if (addr.getAddress().isLinkLocalAddress()) continue;
            ifaceStrBld.append(", ");
            ifaceStrBld.append(addr.toString());
            haveAddr = true;
          }

          if (haveAddr) Utils.log(ifaceStrBld.toString());
        }
        Utils.log("<<<");

        SSLSocketFactory sslSocketFactory = createSocketFactory(originActivity);
        if (sslSocketFactory == null) {
          return false;
        }

        try {
          mSessionId = 0x26c000 + new Random().nextInt(0xFFFF);
          mSocket = sslSocketFactory.createSocket();
          InetSocketAddress remoteAddr =
              new InetSocketAddress(mTivoDevice.addr, mTivoDevice.port);
          mSocket.connect(remoteAddr, TIMEOUT_CONNECT);
          mInputStream = new DataInputStream(mSocket.getInputStream());
          mOutputStream = new DataOutputStream(mSocket.getOutputStream());
        } catch (UnknownHostException e) {
          Utils.logError("connect: unknown host!", e);
          return false;
        } catch (IOException e) {
          Utils.logError("connect: io exception!", e);
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
      Utils.logError("connect: interrupted exception!", e);
      return false;
    } catch (ExecutionException e) {
      Utils.logError("connect: execution exception!", e);
      return false;
    }
  }

  @SuppressLint("TrulyRandom")
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
      Utils.logError("createSocketFactory: CertificateException!", e);
    } catch (IOException e) {
      Utils.logError("createSocketFactory: IOException!", e);
    } catch (KeyManagementException e) {
      Utils.logError("createSocketFactory: KeyManagementException!", e);
    } catch (KeyStoreException e) {
      Utils.logError("createSocketFactory: KeyStoreException!", e);
    } catch (NoSuchAlgorithmException e) {
      Utils.logError("createSocketFactory: NoSuchAlgorithmException!", e);
    } catch (UnrecoverableKeyException e) {
      Utils.logError("createSocketFactory: UnrecoverableKeyException!", e);
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
            Utils.logError("disconnect() socket", e);
          }
        }
        if (mInputStream != null) {
          try {
            mInputStream.close();
          } catch (IOException e) {
            Utils.logError("disconnect() input stream", e);
          }
        }
        if (mOutputStream != null) {
          try {
            mOutputStream.close();
          } catch (IOException e) {
            Utils.logError("disconnect() output stream", e);
          }
        }
      }
    });
    mBodyIsAuthed = false;
    disconnectThread.start();
    try {
      disconnectThread.join();
    } catch (InterruptedException e) {
      Utils.logError("disconnect() interrupted exception", e);
    }
  }

  protected static void dispatchResponse(final MindRpcResponse response) {
    final Integer rpcId = response.getRpcId();
    if (mResponseListenerMap.get(rpcId) == null) {
      return;
    }

    mOriginActivity.runOnUiThread(new Runnable() {
      public void run() {
        MindRpcResponseListener l = mResponseListenerMap.get(rpcId);
        if (l != null) {
          // It might have been removed on another thread, so check for null.
          l.onResponse(response);
        }
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
      // Already connected? No-op.
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
    if (mOriginActivity == null) {
      // We must have been evicted/quit and restarted while at the connect
      // screen which calls us. Restart the app from Now Showing.
      Intent intent =
          new Intent(connectActivity.getBaseContext(), NowShowing.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      connectActivity.startActivity(intent);
      connectActivity.finish();
      return;
    }

    Utils.log("MindRpc.init3() " + mOriginActivity.toString());

    stopThreads();
    disconnect();

    // Try for several seconds to connect, in case e.g. disable-wifi-during
    // -sleep is enabled, thus the connection is being established just as
    // we're being shown, e.g. if we were the last active app being resumed.
    int connectTries = 0;
    while (true) {
      connectTries += 1;
      if (connectTries > 60) {
        settingsError(mOriginActivity, R.string.error_connect, Toast.LENGTH_LONG);
        connectActivity.finish();
        connectActivity.overridePendingTransition(0, 0);
        return;
      }
      if (connect(connectActivity)) {
        break;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        // Ignore.
      }
    }

    mInputThread = new MindRpcInput(mInputStream);
    mInputThread.start();

    mOutputThread = new MindRpcOutput(mOutputStream);
    mOutputThread.start();

    MindRpcResponseListener authListener = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        if ("failure".equals(response.getBody().path("status").asText())) {
          settingsError(connectActivity, R.string.error_auth);
          try {
            mTivoDevice.mak = "";
            new Database(connectActivity).saveDevice(mTivoDevice);
          } catch (Exception e) {
            Utils.logError("Could not remove bad MAK.", e);
          }
          connectActivity.finish();
        } else {
          mBodyIsAuthed = true;
          Intent intent =
              new Intent(connectActivity.getBaseContext(),
                  mOriginActivity.getClass());
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
    addRequest(new BodyAuthenticate(mTivoDevice.mak), authListener);
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
      Utils.logError("readpassword: IOException!", e);
      return "";
    }
  }

  public static void saveBodyId(String bodyId, Context context) {
    if (bodyId == null || bodyId == "" || bodyId == mTivoDevice.tsn) {
      return;
    }

    mTivoDevice.tsn = bodyId;
    new Database(context).saveDevice(mTivoDevice);
  }

  public static void settingsError(
      final Activity activity, final int messageId) {
    settingsError(activity, messageId, Toast.LENGTH_SHORT);
  }

  public static void settingsError(
      final Activity activity, final int messageId, final int toastLen) {
    Utils.log("Settings error: " + activity.getResources().getString(messageId));
    activity.runOnUiThread(new Runnable() {
      public void run() {
        Utils.toast(activity, messageId, toastLen);
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
