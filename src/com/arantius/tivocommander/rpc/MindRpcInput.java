package com.arantius.tivocommander.rpc;

import java.io.BufferedReader;
import java.io.IOException;

import android.util.Log;

import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseFactory;

/**
 * Handle network level input. Generate appropriate response objects.
 */
public class MindRpcInput extends Thread {
  private static final String LOG_TAG = "tivo_commander";

  public volatile boolean mStopFlag = false;
  private final BufferedReader mStream;

  public MindRpcInput(BufferedReader stream) {
    mStream = stream;
  }

  @Override
  public void run() {
    MindRpcResponseFactory mindRpcResponseFactory =
        new MindRpcResponseFactory();

    while (true) {
      if (mStopFlag) {
        break;
      }

      try {
        String respLine = mStream.readLine();
        if (respLine == null) {
          // The socket has closed.
          break;
        }
        if (respLine.length() >= 6 && "MRPC/2".equals(respLine.substring(0, 6))) {
          String[] bytes = respLine.split(" ");
          int headerLen = Integer.parseInt(bytes[1]);
          int bodyLen = Integer.parseInt(bytes[2]);

          char[] headers = new char[headerLen];
          mStream.read(headers, 0, headerLen);

          char[] body = new char[bodyLen];
          int bytesRead = 0;
          while (bytesRead < bodyLen) {
            bytesRead += mStream.read(body, bytesRead, bodyLen - bytesRead);
          }

          final MindRpcResponse response =
              mindRpcResponseFactory.create(headers, body);
          if (response != null) {
            MindRpc.dispatchResponse(response);
          }
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "read: IOException!", e);
        break;
      }
    }
  }
}
