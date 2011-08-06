package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class UpcomingSearch extends CollectionSearch {
  public UpcomingSearch(String collectionId) {
    super(collectionId);

    setReqType("offerSearch");
    mDataMap.put("count", 50);
    mDataMap.put("namespace", "refserver");
    mDataMap.put("note", new String[] { "recordingForOfferId" });
    final String responseTemplateJson =
        "[{\"type\": \"responseTemplate\", \"fieldName\": [\"offer\"], \"typeName\": \"offerList\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"title\", \"subtitle\", \"channel\", \"startTime\", \"recordingForOfferId\", \"purchasableFrom\", \"price\", \"drm\", \"contentId\", \"collectionId\", \"offerId\", \"partnerOfferId\", \"hdtv\", \"repeat\", \"episodic\", \"seasonNumber\", \"episodeNum\", \"transportType\", \"transport\"], \"typeName\": \"offer\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"channelNumber\", \"sourceType\", \"logoIndex\", \"callSign\", \"isDigital\"], \"typeName\": \"channel\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"maxKeepAfterView\", \"maxKeepAfterDownload\"], \"typeName\": \"drm\"}]";
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
    mDataMap.put("searchable", true);

    mDataMap.remove("imageRuleset");
  }
}
