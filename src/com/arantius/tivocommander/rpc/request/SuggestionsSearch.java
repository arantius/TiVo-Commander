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
