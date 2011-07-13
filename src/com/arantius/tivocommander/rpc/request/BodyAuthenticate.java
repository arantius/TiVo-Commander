package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class BodyAuthenticate extends MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  public BodyAuthenticate(String mak) {
    super("bodyAuthenticate");

    try {
      JSONObject credential = new JSONObject();
      credential.put("type", "makCredential");
      credential.put("key", mak);

      mData.put("credential", credential);
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create BodyAuthenticate request", e);
    }
  }
}
