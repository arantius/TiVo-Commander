/*
DVR Commander for TiVo allows control of a TiVo Premiere device.
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

import com.arantius.tivocommander.rpc.MindRpc;
import com.fasterxml.jackson.databind.JsonNode;

public class BaseSearch extends MindRpcRequest {
  public BaseSearch(String collectionId, String contentId) {
    super(""); // We'll figure out type next.

    if (collectionId != null) {
      setReqType("collectionSearch");
      mDataMap.put("collectionId", new String[] { collectionId });
      mDataMap.put("filterUnavailable", false);
    } else if (contentId != null) {
      setReqType("contentSearch");
      mDataMap.put("contentId", new String[] { contentId });
      mDataMap.put("filterUnavailableContent", false);
    }

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("levelOfDetail", "high");
  }

  protected void addCommon(JsonNode imageRuleset, String[] note,
      JsonNode responseTemplate) {
    mDataMap.put("imageRuleset", imageRuleset);
    mDataMap.put("note", note);
    mDataMap.put("responseTemplate", responseTemplate);
  }
}
