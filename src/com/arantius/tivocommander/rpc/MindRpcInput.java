package com.arantius.tivocommander.rpc;

import java.io.BufferedReader;
import java.io.IOException;

import android.util.Log;

import com.arantius.tivocommander.Utils;
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
          String[] respBytes = respLine.split(" ");
          int headerLen = Integer.parseInt(respBytes[1]);
          int bodyLen = Integer.parseInt(respBytes[2]);

          char[] headers = new char[headerLen];
          readBytes(headers, headerLen);

          char[] body = new char[bodyLen];
          readBytes(body, bodyLen);

          final MindRpcResponse response =
              mindRpcResponseFactory.create(headers, body);
          if (response != null) {
            Utils.log(String.format("Got %s response %d",
                response.getRespType(), response.getRpcId()));
            Utils.debugLog(Utils.stringifyToPrettyJson(response.getBody()));
            MindRpc.dispatchResponse(response);
          }
        }
      } catch (IOException e) {
        Log.e(LOG_TAG, "read: IOException!", e);
        break;
      }
    }
  }

  private void readBytes(char[] buf, int len) throws IOException {
    int bytesRead = 0;
    while (bytesRead < len) {
      bytesRead += mStream.read(buf, bytesRead, len - bytesRead);
    }
  }
}
