package com.arantius.tivocommander.rpc.request;

public class CreditsSearch extends BaseSearch {
  // TODO: Make this format less verbose about all the image choices.
  protected final String mImageRulesetJson =
      "[{\"type\": \"imageRuleset\", \"name\": \"personPortrait\", \"rule\": [{\"width\": 113, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"person\"], \"height\": 150}]}]";
  protected final String[] mNote = new String[] {};
  protected final String mResponseTemplateCollJson =
      "[{\"type\": \"responseTemplate\", \"fieldName\": [\"collection\"], \"typeName\": \"collectionList\"}, {\"fieldInfo\": [{\"maxArity\": [50], \"fieldName\": [\"credit\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [               \"collectionId\", \"collectionType\", \"credit\", \"title\"], \"typeName\": \"collection\", \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"first\", \"last\", \"role\", \"image\", \"personId\", \"characterName\"], \"typeName\": \"credit\"}]";
  protected final String mResponseTemplateContJson =
      "[{\"type\": \"responseTemplate\", \"fieldName\": [\"content\"],    \"typeName\": \"contentList\"   }, {\"fieldInfo\": [{\"maxArity\": [50], \"fieldName\": [\"credit\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"contentId\", \"collectionId\", \"collectionType\", \"credit\", \"title\"], \"typeName\": \"content\",    \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"first\", \"last\", \"role\", \"image\", \"personId\", \"characterName\"], \"typeName\": \"credit\"}]";

  public CreditsSearch(String collectionId, String contentId) {
    super(collectionId, contentId);
    addCommon(mImageRulesetJson, mNote,
        collectionId != null ? mResponseTemplateCollJson
            : mResponseTemplateContJson);
  }
}
