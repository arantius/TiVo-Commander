package com.arantius.tivocommander.rpc.request;

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.rpc.MindRpc;

public class SubscriptionSearch extends MindRpcRequest {
  public SubscriptionSearch() {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("format", "idSequence");
    mDataMap.put("levelOfDetail", "low");
    mDataMap.put("noLimit", true);
  }

  public SubscriptionSearch(ArrayList<JsonNode> subscriptionIds) {
    super("subscriptionSearch");
    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("levelOfDetail", "medium");
    mDataMap.put("noLimit", true);
    mDataMap.put("objectIdAndType", subscriptionIds);
  }

  public SubscriptionSearch(String collectionId) {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("collectionId", collectionId);
    mDataMap.put("levelOfDetail", "medium");
  }
}
