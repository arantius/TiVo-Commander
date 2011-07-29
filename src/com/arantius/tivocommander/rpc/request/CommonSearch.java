package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class CommonSearch extends MindRpcRequest {
  protected String mImageRulesetJson = "";
  protected String[] mNote = new String[] {};
  protected String mResponseTemplateJson = "";

  /** Produces an idSequence of shows for the given folder, all if null. */
  public CommonSearch(String type) {
    super(type);
  }

  protected void addCommon(String imageRulesetJson, String[] note,
      String responseTemplateJson) {
    mDataMap.put("bodyId", "-");
    mDataMap.put("filterUnavailableContent", false);
    mDataMap.put("imageRuleset", Utils.parseJson(imageRulesetJson));
    mDataMap.put("levelOfDetail", "high");
    mDataMap.put("note", note);
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
  }
}
