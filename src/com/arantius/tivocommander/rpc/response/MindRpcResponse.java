package com.arantius.tivocommander.rpc.response;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public abstract class MindRpcResponse {
  private final JSONObject mBody;
  private final Boolean mIsFinal;
  private final int mRpcId;

  public MindRpcResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    Log.d("mindrpcresponse", "got: " + bodyObj.toString());
    mBody = bodyObj;
    mIsFinal = isFinal;
    mRpcId = rpcId;
  }

  public Boolean isFinal() {
    return mIsFinal;
  }

  public int getRpcId() {
    return mRpcId;
  }

  public Object get(String key) {
    try {
      return mBody.get(key);
    } catch (JSONException e) {
      return null;
    }
  }
}
