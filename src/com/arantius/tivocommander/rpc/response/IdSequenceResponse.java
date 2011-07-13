package com.arantius.tivocommander.rpc.response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class IdSequenceResponse extends MindRpcResponse {
  private static final String LOG_TAG = "tivo_commander";

  private JSONArray mIds;

  public IdSequenceResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);
    try {
      mIds = bodyObj.getJSONArray("objectIdAndType");
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create IdSequenceResponse; ids", e);
      mIds = new JSONArray();
    }
  }

  public JSONArray getIds() {
    return mIds;
  }
}
