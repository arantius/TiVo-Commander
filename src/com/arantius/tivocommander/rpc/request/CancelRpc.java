package com.arantius.tivocommander.rpc.request;


public class CancelRpc extends MindRpcRequest {
  protected final String mType = "cancel";

  public CancelRpc(int rpcId) {
    super("");
    mRpcId = rpcId;
  }
}
