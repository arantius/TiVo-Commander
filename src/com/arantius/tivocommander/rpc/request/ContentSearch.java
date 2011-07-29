package com.arantius.tivocommander.rpc.request;

public class ContentSearch extends CommonSearch {
  protected final String mImageRulesetJson =
      "[{\"type\":\"imageRuleset\",\"name\":\"movie\",\"rule\":[{\"type\":\"imageRule\",\"width\":133,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"moviePoster\"],\"height\":200}]},{\"type\":\"imageRuleset\",\"name\":\"tvLandscape\",\"rule\":[{\"type\":\"imageRule\",\"width\":139,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"showcaseBanner\"],\"height\":104}]},{\"type\":\"imageRuleset\",\"name\":\"tvPortrait\",\"rule\":[{\"type\":\"imageRule\",\"width\":200,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"showcaseBanner\"],\"height\":150}]}]";
  protected final String[] mNote = new String[] { "userContentForCollectionId",
      "broadbandOfferGroupForContentId", "recordingForContentId" };
  protected final String mResponseTemplateJson =
      "[{\"type\":\"responseTemplate\",\"fieldName\":[\"subscriptionType\"],\"typeName\":\"subscriptionIdentifier\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"state\",\"recordingId\",\"subscriptionIdentifier\"],\"typeName\":\"recording\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"partnerId\",\"contentId\"],\"typeName\":\"offer\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"content\"],\"typeName\":\"contentList\"},{\"type\":\"responseTemplate\",\"fieldInfo\":[{\"type\":\"responseTemplateFieldInfo\",\"maxArity\":[50],\"fieldName\":[\"credit\"]},{\"type\":\"responseTemplateFieldInfo\",\"maxArity\":[2],\"fieldName\":[\"category\"]}],\"fieldName\":[\"contentId\",\"broadbandOfferGroupForContentId\",\"recordingForContentId\",\"collectionId\",\"collectionType\",\"hdtv\",\"title\",\"movieYear\",\"subtitle\",\"seasonNumber\",\"episodeNum\",\"episodic\",\"starRating\",\"description\",\"tvRating\",\"mpaaRating\",\"tvAdvisory\",\"category\",\"credit\",\"originalAirYear\",\"userContentForCollectionId\",\"image\"],\"typeName\":\"content\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"example\"],\"typeName\":\"offerGroup\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"first\",\"last\",\"role\"],\"typeName\":\"credit\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"displayRank\",\"image\"],\"typeName\":\"category\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"thumbsRating\"],\"typeName\":\"userContent\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"channelNumber\",\"sourceType\",\"logoIndex\",\"callSign\",\"isDigital\"],\"typeName\":\"channel\"}]";

  /** Produces an idSequence of shows for the given folder, all if null. */
  public ContentSearch(String contentId) {
    super("contentSearch");
    addCommon(mImageRulesetJson, mNote, mResponseTemplateJson);
    mDataMap.put("contentId", new String[] { contentId });
  }
}
