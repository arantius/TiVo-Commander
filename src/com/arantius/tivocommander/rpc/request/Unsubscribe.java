package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class Unsubscribe extends MindRpcRequest {
  public Unsubscribe(String subscriptionId) {
    super("unsubscribe");
    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("subscriptionId", subscriptionId);
  }
}
