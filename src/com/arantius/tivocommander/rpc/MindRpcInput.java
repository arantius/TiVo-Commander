package com.arantius.tivocommander.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseFactory;

public class MindRpcInput extends Thread {
  private static final String LOG_TAG = "tivo_mindrpc_input";
  public boolean mStopFlag = false;

  private BufferedReader mStream = null;
  private final ConcurrentLinkedQueue<MindRpcResponse> mResponseQueue =
      new ConcurrentLinkedQueue<MindRpcResponse>();

  public void setStream(BufferedReader stream) {
    this.mStream = stream;
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, ">>> run() ...");
    MindRpcResponseFactory mindRpcResponseFactory =
        new MindRpcResponseFactory();

    while (true) {
      if (mStopFlag) {
        Log.d(LOG_TAG, "Got stop flag!");
        break;
      }

      try {
        Log.d(LOG_TAG, "Reading a response ... ");
        String respLine = mStream.readLine();
        if ("MRPC/2".equals(respLine.substring(0, 6))) {
          String[] bytes = respLine.split(" ");
          int headerLen = Integer.parseInt(bytes[1]);
          int bodyLen = Integer.parseInt(bytes[2]);

          char[] headers = new char[headerLen];
          mStream.read(headers, 0, headerLen);

          char[] body = new char[bodyLen];
          mStream.read(body, 0, bodyLen);

          MindRpcResponse response =
              mindRpcResponseFactory.create(headers, body);
          mResponseQueue.add(response);
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "read: IOException!", e);
        break;
      }
    }

    Log.i(LOG_TAG, "<<< run() ...");
  }
}
