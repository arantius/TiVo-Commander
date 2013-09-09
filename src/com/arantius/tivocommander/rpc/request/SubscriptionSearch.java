package com.arantius.tivocommander.rpc.request;

import java.util.ArrayList;

import com.arantius.tivocommander.rpc.MindRpc;

public class SubscriptionSearch extends MindRpcRequest {
  public SubscriptionSearch() {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
    mDataMap.put("format", "idSequence");
    mDataMap.put("levelOfDetail", "low");
    mDataMap.put("noLimit", true);
  }

  public SubscriptionSearch(ArrayList<?> subscriptionIds) {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
    mDataMap.put("levelOfDetail", "medium");
    mDataMap.put("noLimit", true);
    mDataMap.put("objectIdAndType", subscriptionIds);
  }

  public SubscriptionSearch(String collectionId) {
    super("subscriptionSearch");

    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
    mDataMap.put("collectionId", collectionId);
    mDataMap.put("levelOfDetail", "medium");
  }
}
