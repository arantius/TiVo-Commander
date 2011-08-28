package com.arantius.tivocommander.rpc.request;

public class SubscriptionSearch extends MindRpcRequest {
  public SubscriptionSearch(String collectionId) {
    super("subscriptionSearch");

    mDataMap.put("bodyId", mBodyId);
    mDataMap.put("collectionId", collectionId);
    mDataMap.put("levelOfDetail", "medium");
  }
}
