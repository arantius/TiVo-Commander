package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;
import org.json.JSONObject;

public class BodyAuthenticate extends MindRpcRequest {
  public BodyAuthenticate(String mak) {
    super("bodyAuthenticate");

    try {
      JSONObject credential = new JSONObject();
      credential.put("type", "makCredential");
      credential.put("key", mak);

      mData.put("credential", credential);
    } catch (JSONException e) {
    }
  }
}
