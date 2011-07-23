package com.arantius.tivocommander.rpc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.util.Log;

import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;

/**
 * Handle network level output. Accept request objects.
 */
public class MindRpcOutput extends Thread {
  private static final String LOG_TAG = "tivo_commander";

  public volatile boolean mStopFlag = false;
  private volatile ConcurrentLinkedQueue<MindRpcRequest> mRequestQueue =
      new ConcurrentLinkedQueue<MindRpcRequest>();
  private final BufferedWriter mStream;

  public MindRpcOutput(BufferedWriter stream) {
    mStream = stream;
  }

  public void addRequest(MindRpcRequest request) {
    mRequestQueue.add(request);
  }

  @Override
  public void run() {
    while (true) {
      if (mStopFlag) {
        break;
      }

      // Limit worst case battery consumption?
      try {
        Thread.sleep(33);
      } catch (InterruptedException e) {
        break;
      }

      // If necessary, send requests.
      try {
        if (mRequestQueue.peek() != null) {
          MindRpcRequest request = mRequestQueue.remove();
          String reqStr = request.toString();
          mStream.write(reqStr);
          mStream.flush();
          Utils.debugLog("Sent request:\n"
              + Utils.stringifyToPrettyJson(request.getDataMap()));
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "write: io exception!", e);
        break;
      }
    }
  }
}
