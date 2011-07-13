package com.arantius.tivocommander.rpc.response;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class MindRpcResponseFactory {
  private static final String LOG_TAG = "tivo_commander";

  public MindRpcResponse create(char[] headers, char[] body) {
    Boolean isFinal = true;
    int rpcId = 0;

    String line;
    BufferedReader headerReader =
        new BufferedReader(new CharArrayReader(headers));
    try {
      while ((line = headerReader.readLine()) != null) {
        if (line.length() > 9 && "IsFinal:".equals(line.substring(0, 8))) {
          isFinal = "true".equals(line.substring(9));
        } else if (line.length() > 7 && "RpcId:".equals(line.substring(0, 6))) {
          rpcId = Integer.parseInt(line.substring(7));
        }
      }
    } catch (IOException e) {
    }

    JSONObject bodyObj;
    try {
      bodyObj = new JSONObject(new String(body));
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Parse response body", e);
      return null;
    }

    String responseType;
    try {
      responseType = bodyObj.getString("type");
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Parse response body type", e);
      return null;
    }

    if (responseType.equals("bodyAuthenticateResponse")) {
      return new BodyAuthenticateResponse(isFinal, rpcId, bodyObj);
    } else if (responseType.equals("idSequence")) {
      return new IdSequenceResponse(isFinal, rpcId, bodyObj);
    } else if (responseType.equals("recordingFolderItemList")) {
      return new RecordingFolderItemListResponse(isFinal, rpcId, bodyObj);
    } else if (responseType.equals("success")) {
      return new SuccessResponse(isFinal, rpcId, bodyObj);
    } else {
      Log.e(LOG_TAG, "Unknown response type, got data: " + new String(body));
      return null;
    }
  }
}
