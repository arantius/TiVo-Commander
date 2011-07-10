package com.arantius.tivocommander.rpc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.arantius.tivocommander.rpc.request.MindRpcRequest;

public class MindRpcOutput extends Thread {
  private static final String LOG_TAG = "tivo_mindrpc_output";
  public boolean mStopFlag = false;

  private final BufferedWriter mStream;
  private final ConcurrentLinkedQueue<MindRpcRequest> mRequestQueue =
      new ConcurrentLinkedQueue<MindRpcRequest>();

  public MindRpcOutput(BufferedWriter stream) {
    mStream = stream;
  }

  public void addRequest(MindRpcRequest request) {
    mRequestQueue.add(request);
  }

  @Override
  public void run() {
    Log.i(LOG_TAG, ">>> run() ...");

    while (true) {
      if (mStopFlag) {
        Log.d(LOG_TAG, "Got stop flag!");
        break;
      }

      // Limit worst case battery consumption?
      try {
        Thread.sleep(33);
      } catch (InterruptedException e) {
        Log.e(LOG_TAG, "MindRPC sleep was interrupted!", e);
      }

      // If necessary, send requests.
      try {
        if (mRequestQueue.peek() != null) {
          MindRpcRequest request = mRequestQueue.remove();
          String reqStr = request.toString();
          mStream.write(reqStr);
          mStream.flush();
          Log.d(LOG_TAG, "sent request " + request.getDataString());
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "write: io exception!", e);
        break;
      }
    }

    Log.i(LOG_TAG, "<<< run() ...");
  }
}
