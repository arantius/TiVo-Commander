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
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import android.util.Log;

import com.arantius.tivocommander.Main;
import com.arantius.tivocommander.rpc.request.BodyAuthenticate;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseFactory;

public class MindRpc extends Thread {
  private static final String LOG_TAG = "tivo_mindrpc";

  public static volatile Integer mRequestId = 0;
  public static volatile Integer mSessionId = 0;

  private BufferedReader mInputStream = null;
  private BufferedWriter mOutputStream = null;

  private final ConcurrentLinkedQueue<MindRpcRequest> mRequestQueue = new ConcurrentLinkedQueue<MindRpcRequest>();
  private final ConcurrentLinkedQueue<MindRpcResponse> mResponseQueue = new ConcurrentLinkedQueue<MindRpcResponse>();

  protected class AlwaysTrustManager implements X509TrustManager {
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

  public void addRequest(MindRpcRequest request) {
    mRequestQueue.add(request);
  }

  protected void connect() {
    SSLSocketFactory sslSocketFactory = null;

    // Set up the socket factory.
    try {
      TrustManager[] tm = new TrustManager[] { new AlwaysTrustManager() };
      SSLContext context = SSLContext.getInstance("TLS");
      context.init(new KeyManager[0], tm, new SecureRandom());

      sslSocketFactory = context.getSocketFactory();
    } catch (KeyManagementException e) {
      Log.e(LOG_TAG, "ssl: KeyManagementException!", e);
      return;
    } catch (NoSuchAlgorithmException e) {
      Log.e(LOG_TAG, "ssl: NoSuchAlgorithmException!", e);
      return;
    }

    // And use it to create a socket.
    try {
      mSessionId = 0x26c000 + new Random().nextInt(0xFFFF);
      Socket sock = sslSocketFactory.createSocket(Main.mTivoAddr,
          Main.mTivoPort);
      mInputStream = new BufferedReader(new InputStreamReader(
          sock.getInputStream()));
      mOutputStream = new BufferedWriter(new OutputStreamWriter(
          sock.getOutputStream()));
    } catch (UnknownHostException e) {
      Log.i(LOG_TAG, "connect: unknown host!", e);
      return;
    } catch (IOException e) {
      Log.e(LOG_TAG, "connect: io exception!", e);
      return;
    }
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, ">>> MindRPC run() ...");

    MindRpcResponseFactory mindRpcResponseFactory = new MindRpcResponseFactory();
    connect();
    addRequest(new BodyAuthenticate());

    while (true) {
      // Limit worst case battery consumption?
      try {
        Thread.sleep(333);
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "MindRPC sleep was interrupted!", e);
      }

      // If necessary, send requests.
      try {
        if (mRequestQueue.peek() != null) {
          Log.d(LOG_TAG, ">>> make request");
          MindRpcRequest request = mRequestQueue.remove();
          String reqStr = request.toString();
          mOutputStream.write(reqStr);
          mOutputStream.flush();
          Log.d(LOG_TAG, "<<< make request");
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "write: io exception!", e);
      }

      // If necessary, read responses.
      try {
        Log.d(LOG_TAG, "Reading a response ... ");
        String respLine = mInputStream.readLine();
        if ("MRPC/2".equals(respLine.substring(0, 6))) {
          String[] bytes = respLine.split(" ");
          int headerLen = Integer.parseInt(bytes[1]);
          int bodyLen = Integer.parseInt(bytes[2]);

          char[] headers = new char[headerLen];
          mInputStream.read(headers, 0, headerLen);

          char[] body = new char[bodyLen];
          mInputStream.read(body, 0, bodyLen);

          MindRpcResponse response = mindRpcResponseFactory.create(headers,
              body);
        }
        return;
      } catch (IOException e) {
        // TODO Auto-generated catch block
        Log.e(LOG_TAG, "read: IOException!", e);
      }
    }
  }
}
