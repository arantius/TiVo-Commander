package com.arantius.tivocommander.rpc.request;


public class OfferSearch extends MindRpcRequest {
  public OfferSearch() {
    super("offerSearch");

    mDataMap.put("bodyId", "-");
    mDataMap.put("count", 50);
    mDataMap.put("namespace", "refserver");
    mDataMap.put("searchable", true);
  }

  public void setChannelsForCollection(String collectionId) {
    mDataMap.put("collectionId", new String[] { collectionId });
    mDataMap.put("groupBy", new String[] { "channelNumber" });
    mDataMap.put("orderBy", new String[] { "channelNumber" });
  }
}
