package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class SuggestionsSearch extends CollectionSearch {
  public SuggestionsSearch(String collectionId) {
    super(collectionId);

    final String imageRulsetJson =
        "[{\"type\": \"imageRuleset\", \"name\": \"movie\", \"rule\": [{\"width\": 100, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"moviePoster\"], \"height\": 150}]}, {\"type\": \"imageRuleset\", \"name\": \"tvPortrait\", \"rule\": [{\"width\": 120, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"showcaseBanner\"], \"height\": 90}]}]";
    mDataMap.put("imageRuleset", Utils.parseJson(imageRulsetJson));

    mDataMap.put("note",
        new String[] { "bodyLineupCorrelatedCollectionForCollectionId" });

    final String responseTemplateJson =
        "[{\"type\": \"responseTemplate\", \"fieldName\": [\"collection\"], \"typeName\": \"collectionList\"}, {\"fieldInfo\": [{\"maxArity\": [2], \"fieldName\": [\"category\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"image\", \"title\", \"collectionId\", \"collectionType\", \"correlatedCollectionForCollectionId\"], \"typeName\": \"collection\", \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"displayRank\", \"image\"], \"typeName\": \"category\"}] ";
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
  }
}
