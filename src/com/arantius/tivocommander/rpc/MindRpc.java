package com.arantius.tivocommander.rpc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Random;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.arantius.tivocommander.R;
import com.arantius.tivocommander.Settings;
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

  private static final String LOG_TAG = "tivo_commander";
  private static BufferedReader mInputStream;
  private static MindRpcInput mInputThread;
  private static Activity mOriginActivity;
  private static BufferedWriter mOutputStream;
  private static MindRpcOutput mOutputThread;
  private static HashMap<Integer, MindRpcResponseListener> mResponseListenerMap =
      new HashMap<Integer, MindRpcResponseListener>();
  private static volatile int mRpcId = 1;
  private static volatile int mSessionId;
  private static Socket mSocket;
  private static String mTivoAddr;
  private static String mTivoMak;
  private static int mTivoPort;

  /**
   * Add an outgoing request to the queue.
   *
   * @param request The requestequest to be sent.
   * @param listener The object to notify when the response(s) come back.
   */
  public static void addRequest(MindRpcRequest request,
      MindRpcResponseListener listener) {
    mOutputThread.addRequest(request);
    if (listener != null) {
      mResponseListenerMap.put(request.getRpcId(), listener);
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
        if (response.get("status").equals("failure")) {
          settingsError(originActivity, R.string.error_auth);
        }
      }
    });
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

    return true;
  }

  private static boolean connect() {
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
      mSocket = sslSocketFactory.createSocket(mTivoAddr, mTivoPort);
      mInputStream =
          new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
      mOutputStream =
          new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
    } catch (UnknownHostException e) {
      Log.i(LOG_TAG, "connect: unknown host!", e);
      return false;
    } catch (IOException e) {
      Log.e(LOG_TAG, "connect: io exception!", e);
      return false;
    }

    return true;
  }

  private static void disconnect() {
    if (mSocket != null) {
      try {
        mSocket.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "disconnect()", e);
      }
    }
    if (mInputStream != null) {
      try {
        mInputStream.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "disconnect()", e);
      }
    }
    if (mOutputStream != null) {
      try {
        mOutputStream.close();
      } catch (IOException e) {
        Log.e(LOG_TAG, "disconnect()", e);
      }
    }
  }

  private static void settingsError(Activity activity, int messageId) {
    stopThreads();
    disconnect();
    Toast.makeText(activity.getBaseContext(), messageId,
        Toast.LENGTH_SHORT).show();
    Intent i = new Intent(activity.getBaseContext(), Settings.class);
    activity.startActivity(i);
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
