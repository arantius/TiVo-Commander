package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class PersonCreditsSearch extends MindRpcRequest {
  /** Produces an idSequence of shows for the given folder, all if null. */
  public PersonCreditsSearch(String personId) {
    super("collectionSearch");

    final String creditJson =
        "[{\"personId\": \"" + personId + "\", \"type\": \"credit\"}]";
    final String imageRulesetJson =
        "[{\"type\": \"imageRuleset\", \"name\": \"movie\", \"rule\": [{\"width\": 100, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"moviePoster\"], \"height\": 150}]}, {\"type\": \"imageRuleset\", \"name\": \"tvLandscape\", \"rule\": [{\"width\": 139, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"showcaseBanner\"], \"height\": 104}]}]";
    final String responseTemplateJson =
        "[{\"type\": \"responseTemplate\", \"fieldName\": [\"collection\"], \"typeName\": \"collectionList\"}, {\"fieldInfo\": [{\"maxArity\": [50], \"fieldName\": [\"credit\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"image\", \"title\", \"collectionId\", \"collectionType\", \"episodic\", \"mpaaRating\", \"tvRating\", \"starRating\"], \"typeName\": \"collection\", \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"role\", \"personId\"], \"typeName\": \"credit\"}]";

    mDataMap.put("bodyId", "-");
    mDataMap.put("count", 50);
    mDataMap.put("credit", Utils.parseJson(creditJson));
    mDataMap.put("imageRuleset", Utils.parseJson(imageRulesetJson));
    mDataMap.put("levelOfDetail", "high");
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
  }
}
