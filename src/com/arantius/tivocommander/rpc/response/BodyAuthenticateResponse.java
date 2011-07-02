package com.arantius.tivocommander.rpc.response;

import org.json.JSONObject;

public class BodyAuthenticateResponse extends MindRpcResponse {
  public BodyAuthenticateResponse(Boolean isFinal, int rpcId,
      JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);
    // TODO Auto-generated constructor stub
  }
}
