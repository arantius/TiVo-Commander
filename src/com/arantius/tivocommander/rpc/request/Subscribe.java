package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;

public class Subscribe extends MindRpcRequest {
  public Subscribe(String offerId, String contentId) {
    super("subscribe");

    HashMap<String, String> idSetSource = new HashMap<String, String>();
    // Channel necessary?
    idSetSource.put("contentId", contentId);
    idSetSource.put("offerId", offerId);
    idSetSource.put("type", "singleOfferSource");

    mDataMap.put("bodyId", "-");
    mDataMap.put("endTimePadding", 0);
    mDataMap.put("idSetSource", idSetSource);
    mDataMap.put("keepBehavior", "duration");
    mDataMap.put("keepDuration", 86400);
    mDataMap.put("maxRecordings", 5);
    mDataMap.put("recordingQuality", "best");
    mDataMap.put("showStatus", "rerunsAllowed");
    mDataMap.put("startTimePadding", 0);
  }
}
