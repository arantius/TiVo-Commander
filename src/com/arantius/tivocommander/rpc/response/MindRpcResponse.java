package com.arantius.tivocommander.rpc.response;

import org.codehaus.jackson.JsonNode;

public class MindRpcResponse {
  private static final String LOG_TAG = "tivo_commander";

  private final JsonNode mBody;
  private final Boolean mIsFinal;
  private final int mRpcId;

  public MindRpcResponse(Boolean isFinal, int rpcId, JsonNode bodyObj) {
    mBody = bodyObj;
    mIsFinal = isFinal;
    mRpcId = rpcId;
  }

  public JsonNode getBody() {
    return mBody;
  }

  public int getRpcId() {
    return mRpcId;
  }

  public Boolean isFinal() {
    return mIsFinal;
  }
}
