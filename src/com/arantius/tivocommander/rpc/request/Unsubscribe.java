package com.arantius.tivocommander.rpc.request;

public class Unsubscribe extends MindRpcRequest {
  public Unsubscribe(String subscriptionId) {
    super("unsubscribe");
    mDataMap.put("bodyId", "-");
    mDataMap.put("subscriptionId", subscriptionId);
  }
}
