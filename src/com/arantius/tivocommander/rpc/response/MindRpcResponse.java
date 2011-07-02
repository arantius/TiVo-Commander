package com.arantius.tivocommander.rpc.response;

import org.json.JSONObject;

import android.util.Log;

public abstract class MindRpcResponse {
  public JSONObject mBody;
  public Boolean mIsFinal;
  public int mRpcId;

  public MindRpcResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    Log.d("mindrpcresponse", "got: " + bodyObj.toString());
    mBody = bodyObj;
    mIsFinal = isFinal;
    mRpcId = rpcId;
  }
}
