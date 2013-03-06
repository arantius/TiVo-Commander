package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class OfferSearch extends MindRpcRequest {
  public OfferSearch() {
    super("offerSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("count", 50);
    mDataMap.put("namespace", "refserver");
    mDataMap.put("searchable", true);
  }

  public OfferSearch(String searchKey, String searchVal) {
    super("offerSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("levelOfDetail", "low");
    mDataMap.put("namespace", "refserver");
    mDataMap.put("note", new String[] { "recordingForContentId" });
    mDataMap.put(searchKey, new String[] { searchVal });
    mDataMap.put("searchable", true);
  }

  public void setChannelsForCollection(String collectionId) {
    mDataMap.put("collectionId", new String[] { collectionId });
    mDataMap.put("groupBy", new String[] { "channelNumber" });
    mDataMap.put("orderBy", new String[] { "channelNumber" });
  }
}
