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

public class UpcomingSearch extends CollectionSearch {
  private static final JsonNode mResponseTemplate =
      Utils
          .parseJson("[{\"type\": \"responseTemplate\", \"fieldName\": [\"offer\"], \"typeName\": \"offerList\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"title\", \"subtitle\", \"channel\", \"startTime\", \"recordingForOfferId\", \"purchasableFrom\", \"price\", \"drm\", \"contentId\", \"collectionId\", \"offerId\", \"partnerOfferId\", \"hdtv\", \"repeat\", \"episodic\", \"seasonNumber\", \"episodeNum\", \"transportType\", \"transport\"], \"typeName\": \"offer\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"channelNumber\", \"sourceType\", \"logoIndex\", \"callSign\", \"isDigital\"], \"typeName\": \"channel\"}, {\"type\": \"responseTemplate\", \"fieldName\": [\"maxKeepAfterView\", \"maxKeepAfterDownload\"], \"typeName\": \"drm\"}]");
  private static final String[] mNote = new String[] { "recordingForOfferId" };

  public UpcomingSearch(String collectionId) {
    super(collectionId);

    setReqType("offerSearch");
    mDataMap.put("count", 50);
    mDataMap.put("namespace", "refserver");
    mDataMap.put("note", mNote);
    mDataMap.put("responseTemplate", mResponseTemplate);
    mDataMap.put("searchable", true);

    mDataMap.remove("imageRuleset");
  }
}
