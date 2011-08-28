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

public class PersonCreditsSearch extends MindRpcRequest {
  private static final JsonNode mImageRuleset =
      Utils
          .parseJson("[{\"type\": \"imageRuleset\", \"name\": \"movie\", \"rule\": [{\"width\": 100, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"moviePoster\"], \"height\": 150}]}, {\"type\": \"imageRuleset\", \"name\": \"tvLandscape\", \"rule\": [{\"width\": 139, \"ruleType\": \"exactMatchDimension\", \"type\": \"imageRule\", \"imageType\": [\"showcaseBanner\"], \"height\": 104}]}]");
  private static final JsonNode mResponseTemplate =
      Utils
          .parseJson("[{\"type\": \"responseTemplate\", \"fieldName\": [\"collection\"], \"typeName\": \"collectionList\"}, {\"fieldInfo\": [{\"maxArity\": [50], \"fieldName\": [\"credit\"], \"type\": \"responseTemplateFieldInfo\"}], \"fieldName\": [\"image\", \"title\", \"collectionId\"], \"typeName\": \"collection\", \"type\": \"responseTemplate\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"role\", \"personId\"], \"typeName\": \"credit\"}]");

  public PersonCreditsSearch(String personId) {
    super("collectionSearch");

    final String creditJson =
        "[{\"personId\": \"" + personId + "\", \"type\": \"credit\"}]";

    mDataMap.put("bodyId", mBodyId);
    mDataMap.put("count", 50);
    mDataMap.put("credit", Utils.parseJson(creditJson));
    mDataMap.put("imageRuleset", mImageRuleset);
    mDataMap.put("levelOfDetail", "high");
    mDataMap.put("responseTemplate", mResponseTemplate);
  }
}
