package com.arantius.tivocommander.rpc.request;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.Utils;

public class RecordingFolderItemSearch extends MindRpcRequest {
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

    final String responseTemplateJson =
        "[{\"type\":\"responseTemplate\",\"fieldName\":[\"recordingFolderItem\"],\"typeName\":\"recordingFolderItemList\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"folderInProgress\",\"folderTransportType\",\"folderType\",\"recordingFolderItemId\",\"recordingForChildRecordingId\",\"folderItemCount\",\"recordingStatusType\",\"startTime\",\"title\",\"transportType\",\"childRecordingId\"],\"typeName\":\"recordingFolderItem\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"channel\",\"contentId\",\"collectionId\",\"hdtv\",\"episodic\",\"repeat\",\"startTime\"],\"typeName\":\"recording\"},{\"type\":\"responseTemplate\",\"fieldName\":[\"logoIndex\",\"callSign\",\"channelNumber\"],\"typeName\":\"channel\"}]";
    mDataMap.put("responseTemplate", Utils.parseJson(responseTemplateJson));
  }
}
