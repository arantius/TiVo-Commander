package com.arantius.tivocommander.rpc.response;

import org.json.JSONObject;


public class SuccessResponse extends MindRpcResponse {
  public SuccessResponse(Boolean isFinal, int rpcId, JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);
    // TODO Auto-generated constructor stub
  }
}
