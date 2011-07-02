package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;
import org.json.JSONObject;

import com.arantius.tivocommander.Main;

public class BodyAuthenticate extends MindRpcRequest {
  public BodyAuthenticate() {
    super("bodyAuthenticate");

    try {
      JSONObject credential = new JSONObject();
      credential.put("type", "makCredential");
      credential.put("key", Main.mTivoMak);

      mData.put("credential", credential);
    } catch (JSONException e) {
    }
  }
}
