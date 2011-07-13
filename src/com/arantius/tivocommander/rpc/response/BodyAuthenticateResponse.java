package com.arantius.tivocommander.rpc.response;

import org.json.JSONException;
import org.json.JSONObject;

public class BodyAuthenticateResponse extends MindRpcResponse {
  private String mMessage;
  private String mStatus;

  public BodyAuthenticateResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);
    try {
      mMessage = bodyObj.getString("message");
    } catch (JSONException e) {
      mMessage = "JSON Failure";
    }

    try {
      mStatus = bodyObj.getString("status");
    } catch (JSONException e) {
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
