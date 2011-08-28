package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class SubscriptionSearch extends MindRpcRequest {
  public SubscriptionSearch(String collectionId) {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("collectionId", collectionId);
    mDataMap.put("levelOfDetail", "medium");
  }
}
