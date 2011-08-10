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
  private static final JsonNode imageRuleset =
      Utils
          .parseJson("[{\"type\":\"imageRuleset\",\"name\":\"movie\",\"rule\":[{\"type\":\"imageRule\",\"width\":100,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"moviePoster\"],\"height\":150}]},{\"type\":\"imageRuleset\",\"name\":\"tvLandscape\",\"rule\":[{\"type\":\"imageRule\",\"width\":139,\"ruleType\":\"exactMatchDimension\",\"imageType\":[\"showcaseBanner\"],\"height\":104}]}]");
  private static final String[] mNote = new String[] { "collection" };
  private static final JsonNode responseTemplate =
      Utils
          .parseJson("[{\"type\":\"responseTemplate\",\"fieldName\":[\"image\",\"title\",\"collectionId\"],\"typeName\":\"collection\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"image\",\"title\",\"collectionId\"],\"typeName\":\"content\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"unifiedItem\"],\"typeName\":\"unifiedItemList\"}]");

  public UnifiedItemSearch(String keyword) {
    super("unifiedItemSearch");

    mDataMap.put("bodyId", "-");
    mDataMap.put("count", 5);
    mDataMap.put("responseTemplate", imageRuleset);
    mDataMap.put("includeUnifiedItemType", mNote);
    mDataMap.put("keyword", keyword);
    mDataMap.put("levelOfDetail", "medium");
    mDataMap.put("numRelevantItems", 50);
    mDataMap.put("orderBy", new String[] { "relevance" });
    mDataMap.put("responseTemplate", responseTemplate);
    mDataMap.put("searchable", true);
  }
}
