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

public class RecordingFolderItemSearch extends MindRpcRequest {
  private static final String mResponseTemplateJson =
      "[{\"type\":\"responseTemplate\",\"fieldName\":[\"recordingFolderItem\"],\"typeName\":\"recordingFolderItemList\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"folderInProgress\",\"folderTransportType\",\"folderType\",\"recordingFolderItemId\",\"recordingForChildRecordingId\",\"folderItemCount\",\"recordingStatusType\",\"startTime\",\"title\",\"transportType\",\"childRecordingId\"],\"typeName\":\"recordingFolderItem\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"channel\",\"contentId\",\"collectionId\",\"hdtv\",\"episodic\",\"repeat\",\"startTime\"],\"typeName\":\"recording\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"logoIndex\",\"callSign\",\"channelNumber\"],\"typeName\":\"channel\"}]";

  /** Produces an idSequence of shows for the given folder, all if null. */
  public RecordingFolderItemSearch(String folderId) {
    super("recordingFolderItemSearch");

    addCommonDetails();
    mDataMap.put("format", "idSequence");
    if (folderId != null) {
      mDataMap.put("parentRecordingFolderItemId", folderId);
    }
  }

  /** Given a set of IDs, produces details about the shows. */
  public RecordingFolderItemSearch(JsonNode showIds) {
    super("recordingFolderItemSearch");

    addCommonDetails();
    mDataMap.put("objectIdAndType", showIds);
  }

  private void addCommonDetails() {
    mDataMap.put("orderBy", new String[] { "startTime" });
    mDataMap.put("bodyId", "-");
    mDataMap.put("note", new String[] { "recordingForChildRecordingId" });
    mDataMap.put("responseTemplate", Utils.parseJson(mResponseTemplateJson));
  }
}
