package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class BaseSearch extends MindRpcRequest {
  public BaseSearch(String collectionId, String contentId) {
    super(""); // We'll figure out type next.

    if (collectionId != null) {
      setReqType("collectionSearch");
      mDataMap.put("collectionId", new String[] { collectionId });
      mDataMap.put("filterUnavailable", false);
    } else if (contentId != null) {
      setReqType("contentSearch");
      mDataMap.put("contentId", new String[] { contentId });
      mDataMap.put("filterUnavailableContent", false);
    }

    mDataMap.put("bodyId", "-");
    mDataMap.put("levelOfDetail", "high");
  }

  protected void addCommon(String imageRulesetJson, String[] note,
      String responseTemplateJson) {
    mDataMap.put("imageRuleset", Utils.parseJson(imageRulesetJson));
    mDataMap.put("note", note);
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
  }
}
