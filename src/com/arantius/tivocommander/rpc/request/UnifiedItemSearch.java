/*
TiVo Commander allows control of a TiVo Premiere device.
Copyright (C) 2011  Anthony Lieuallen (arantius@gmail.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.arantius.tivocommander.rpc.request;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.Utils;

public class UnifiedItemSearch extends MindRpcRequest {
  private static final int NUM_RESULTS = 25;
  private static final JsonNode mImageRuleset =
      Utils
          .parseJson("[{\"type\": \"imageRuleset\", \"name\": \"movie\", \"rule\": [{\"width\": 100, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"moviePoster\"], \"height\": 150}]}, {\"type\": \"imageRuleset\", \"name\": \"tvLandscape\", \"rule\": [{\"width\": 139, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"showcaseBanner\"], \"height\": 104}]}, {\"type\": \"imageRuleset\", \"name\": \"tvPortrait\", \"rule\": [{\"width\": 120, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"showcaseBanner\"], \"height\": 90}]}, {\"type\": \"imageRuleset\", \"name\": \"personLandscape\", \"rule\": [{\"width\": 104, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"person\"], \"height\": 78}]}, {\"type\": \"imageRuleset\", \"name\": \"personPortrait\", \"rule\": [{\"width\": 113, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"person\"], \"height\": 150}]}]");
  private static final JsonNode mResponseTemplate =
      Utils
          .parseJson("[{\"fieldInfo\": [{\"maxArity\": [2], \"fieldName\": [\"category\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"image\", \"title\", \"collectionId\", \"collectionType\", \"movieYear\", \"starRating\", \"tvRating\", \"mpaaRating\"], \"typeName\": \"collection\", \"type\": \"responseTemplate\"}, {\"fieldInfo\": [{\"maxArity\": [2], \"fieldName\": [\"category\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"image\", \"title\", \"subtitle\", \"collectionId\", \"collectionType\", \"contentId\", \"movieYear\", \"starRating\", \"tvRating\", \"mpaaRating\"], \"typeName\": \"content\", \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"displayRank\", \"image\"], \"typeName\": \"category\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"first\", \"last\", \"image\", \"personId\"], \"typeName\": \"person\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"unifiedItem\"], \"typeName\": \"unifiedItemList\"}]");

  public UnifiedItemSearch(String keyword) {
    super("unifiedItemSearch");

    mDataMap.put("bodyId", mBodyId);
    mDataMap.put("count", NUM_RESULTS);
    mDataMap.put("imageRuleset", mImageRuleset);
    mDataMap.put("includeUnifiedItemType", new String[] { "collection",
        "content", "person" });
    mDataMap.put("keyword", keyword);
    mDataMap.put("levelOfDetail", "medium");
    mDataMap.put("mergeOverridingCollections", true);
    mDataMap.put("numRelevantItems", NUM_RESULTS);
    mDataMap.put("orderBy", new String[] { "relevance" });
    mDataMap.put("responseTemplate", mResponseTemplate);
    mDataMap.put("searchable", true);
  }
}
