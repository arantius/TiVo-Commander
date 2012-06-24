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

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.MindRpc;

public class RecordingFolderItemSearch extends MindRpcRequest {
  private static final JsonNode mResponseTemplate =
      Utils
          .parseJson("[{\"type\":\"responseTemplate\",\"fieldName\":[\"recordingFolderItem\"],\"typeName\":\"recordingFolderItemList\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"folderTransportType\",\"folderType\",\"recordingFolderItemId\",\"recordingForChildRecordingId\",\"folderItemCount\",\"recordingStatusType\",\"startTime\",\"title\",\"childRecordingId\"],\"typeName\":\"recordingFolderItem\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"contentId\",\"collectionId\",\"hdtv\",\"channel\",\"startTime\"],\"typeName\":\"recording\"}]");

  /** Produces an idSequence of shows for the given folder, all if null. */
  public RecordingFolderItemSearch(String folderId, String orderBy) {
    super("recordingFolderItemSearch");

    addCommonDetails(orderBy);
    mDataMap.put("format", "idSequence");
    if (folderId != null) {
      mDataMap.put("parentRecordingFolderItemId", folderId);
    }
  }

  /** Given a set of IDs, produces details about the shows. */
  public RecordingFolderItemSearch(JsonNode showIds, String orderBy) {
    super("recordingFolderItemSearch");

    addCommonDetails(orderBy);
    mDataMap.put("objectIdAndType", showIds);
  }

  /** Given a set of IDs, produces details about the shows. */
  public RecordingFolderItemSearch(ArrayList<JsonNode> showIds, String orderBy) {
    super("recordingFolderItemSearch");

    addCommonDetails(orderBy);
    mDataMap.put("objectIdAndType", showIds);
  }

  private void addCommonDetails(String orderBy) {
    mDataMap.put("orderBy", new String[] { orderBy });
    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("note", new String[] { "recordingForChildRecordingId" });
    mDataMap.put("responseTemplate", mResponseTemplate);
  }
}
