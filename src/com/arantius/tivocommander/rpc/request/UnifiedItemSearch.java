package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class UnifiedItemSearch extends MindRpcRequest {
  /** Produces an idSequence of shows for the given folder, all if null. */
  public UnifiedItemSearch(String keyword) {
    super("unifiedItemSearch");

    mDataMap.put("bodyId", "-");
    mDataMap.put("count", 5);
    final String imageRulesetJson =
        "[{\"type\":\"imageRuleset\",\"name\":\"movie\",\"rule\":[{\"type\":\"imageRule\",\"width\":100,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"moviePoster\"],\"height\":150}]},{\"type\":\"imageRuleset\",\"name\":\"tvLandscape\",\"rule\":[{\"type\":\"imageRule\",\"width\":139,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"showcaseBanner\"],\"height\":104}]},{\"type\":\"imageRuleset\",\"name\":\"tvPortrait\",\"rule\":[{\"type\":\"imageRule\",\"width\":120,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"showcaseBanner\"],\"height\":90}]},{\"type\":\"imageRuleset\",\"name\":\"personLandscape\",\"rule\":[{\"type\":\"imageRule\",\"width\":104,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"person\"],\"height\":78}]},{\"type\":\"imageRuleset\",\"name\":\"personPortrait\",\"rule\":[{\"type\":\"imageRule\",\"width\":113,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"person\"],\"height\":150}]}]";
    mDataMap.put("responseTemplate", Utils.parseJson(imageRulesetJson));
    mDataMap.put("includeUnifiedItemType", new String[] { "collection",
        "content", "person" });
    mDataMap.put("keyword", keyword);
    mDataMap.put("levelOfDetail", "medium");
//    mDataMap.put("minEndTime", "2011-07-28 20:44:24");  // now + 4 hours??
    mDataMap.put("numRelevantItems", 50);
    mDataMap.put("orderBy", new String[] { "relevance" });
    final String responseTemplateJson =
        "[{\"type\":\"responseTemplate\",\"fieldInfo\":[{\"type\":\"responseTemplateFieldInfo\",\"maxArity\":[2],\"fieldName\":[\"category\"]}],\"fieldName\":[\"image\",\"title\",\"collectionId\",\"collectionType\",\"movieYear\",\"starRating\",\"tvRating\",\"mpaaRating\"],\"typeName\":\"collection\"},{\"type\":\"responseTemplate\",\"fieldInfo\":[{\"type\":\"responseTemplateFieldInfo\",\"maxArity\":[2],\"fieldName\":[\"category\"]}],\"fieldName\":[\"image\",\"title\",\"subtitle\",\"collectionId\",\"collectionType\",\"contentId\",\"movieYear\",\"starRating\",\"tvRating\",\"mpaaRating\"],\"typeName\":\"content\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"displayRank\",\"image\"],\"typeName\":\"category\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"first\",\"last\",\"image\",\"personId\"],\"typeName\":\"person\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"unifiedItem\"],\"typeName\":\"unifiedItemList\"}]";
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
    mDataMap.put("searchable", true);
  }
}
