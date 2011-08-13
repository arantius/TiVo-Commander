package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;

public class Subscribe extends MindRpcRequest {
  public Subscribe() {
    super("subscribe");

    mDataMap.put("bodyId", "-");
    mDataMap.put("recordingQuality", "best");
  }

  public void setKeep(String behavior, Integer duration) {
    mDataMap.put("keepBehavior", behavior);
    if ("duration".equals(behavior)) {
      mDataMap.put("keepDuration", duration);
    }
  }

  public void setOffer(String offerId, String contentId) {
    HashMap<String, String> idSetSource = new HashMap<String, String>();
    // Channel necessary?
    idSetSource.put("contentId", contentId);
    idSetSource.put("offerId", offerId);
    idSetSource.put("type", "singleOfferSource");
    mDataMap.put("idSetSource", idSetSource);
  }

  public void setPadding(Integer start, Integer stop) {
    if (start != null) {
      mDataMap.put("startTimePadding", start);
    }
    if (stop != null) {
      mDataMap.put("endTimePadding", stop);
    }
  }
}
