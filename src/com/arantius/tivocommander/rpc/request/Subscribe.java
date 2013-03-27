package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;

import com.arantius.tivocommander.rpc.MindRpc;
import com.fasterxml.jackson.databind.JsonNode;

public class Subscribe extends MindRpcRequest {
  public Subscribe() {
    super("subscribe");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("recordingQuality", "best");
  }

  public void setCollection(String collectionId, JsonNode channel, int max,
      String which, String subscriptionId) {
    HashMap<String, Object> idSetSource = new HashMap<String, Object>();
    idSetSource.put("channel", channel);
    idSetSource.put("collectionId", collectionId);
    idSetSource.put("type", "seasonPassSource");
    if (subscriptionId != null && !"".equals(subscriptionId)) {
      mDataMap.put("subscriptionId", subscriptionId);
    }
    mDataMap.put("idSetSource", idSetSource);
    mDataMap.put("maxRecordings", max);
    mDataMap.put("showStatus", which);
  }

  public void setIgnoreConflicts(boolean ignoreConflicts) {
    mDataMap.put("ignoreConflicts", ignoreConflicts);
  }

  public void setKeepUntil(String behavior) {
    mDataMap.put("keepBehavior", behavior);
  }

  public void setOffer(String offerId, String contentId) {
    HashMap<String, String> idSetSource = new HashMap<String, String>();
    idSetSource.put("contentId", contentId);
    idSetSource.put("offerId", offerId);
    idSetSource.put("type", "singleOfferSource");
    mDataMap.put("idSetSource", idSetSource);
  }

  public void setPadding(Integer start, Integer stop) {
    if (start != 0) {
      mDataMap.put("startTimePadding", start);
    }
    if (stop != 0) {
      mDataMap.put("endTimePadding", stop);
    }
  }

  public void setPriority(Integer priority) {
    if (priority > 0) {
      mDataMap.put("priority", priority);
    }
  }
}
