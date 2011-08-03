package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.Utils;

public class PersonSearch extends MindRpcRequest {
  public PersonSearch(String personId) {
    super("personSearch");

    final String imageRulesetJson =
        "[{\"type\": \"imageRuleset\", \"name\": \"person\", \"rule\": [{\"width\": 150, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"person\"], \"height\": 200}]}]";
    mDataMap.put("imageRuleset", Utils.parseJson(imageRulesetJson));
    mDataMap.put("levelOfDetail", "high");
    mDataMap.put("note", new String[] { "roleForPersonId" });
    mDataMap.put("personId", new String[] { personId });
  }
}
