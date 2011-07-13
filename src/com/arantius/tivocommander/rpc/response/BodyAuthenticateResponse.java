package com.arantius.tivocommander.rpc.response;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BodyAuthenticateResponse extends MindRpcResponse {
  private static final String LOG_TAG = "tivo_commander";

  private String mMessage;
  private String mStatus;

  public BodyAuthenticateResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);
    try {
      mMessage = bodyObj.getString("message");
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create BodyAuthenticateResponse; message", e);
      mMessage = "JSON Failure";
    }

    try {
      mStatus = bodyObj.getString("status");
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create BodyAuthenticateResponse; status", e);
      mStatus = "JSON Failure";
    }
  }

  public String getMessage() {
    return mMessage;
  }

  public String getStatus() {
    return mStatus;
  }
}
