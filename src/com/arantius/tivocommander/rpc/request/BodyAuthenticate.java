package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;
import java.util.Map;

public class BodyAuthenticate extends MindRpcRequest {
  public BodyAuthenticate(String mak) {
    super("bodyAuthenticate");

    Map<String, String> credential = new HashMap<String, String>();
    credential.put("type", "makCredential");
    credential.put("key", mak);
    mDataMap.put("credential", credential);
  }
}
