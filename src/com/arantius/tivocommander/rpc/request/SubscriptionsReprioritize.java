package com.arantius.tivocommander.rpc.request;

import java.util.ArrayList;

import com.arantius.tivocommander.rpc.MindRpc;

public class SubscriptionsReprioritize extends MindRpcRequest {

  public SubscriptionsReprioritize(ArrayList<String> subscriptionIds) {
    super("subscriptionsReprioritize");
    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("subscriptionId", subscriptionIds);
  }
}
