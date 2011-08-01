package com.arantius.tivocommander.rpc.response;

import org.codehaus.jackson.JsonNode;

public class MindRpcResponse {
  private final JsonNode mBody;
  private final Boolean mIsFinal;
  private final int mRpcId;
  private final String mRespType;

  public MindRpcResponse(Boolean isFinal, int rpcId, JsonNode bodyObj) {
    mBody = bodyObj;
    mIsFinal = isFinal;
    mRpcId = rpcId;
    mRespType = bodyObj.path("type").getTextValue();
  }

  public JsonNode getBody() {
    return mBody;
  }

  public int getRpcId() {
    return mRpcId;
  }

  public String getRespType() {
    return mRespType;
  }

  public Boolean isFinal() {
    return mIsFinal;
  }
}
