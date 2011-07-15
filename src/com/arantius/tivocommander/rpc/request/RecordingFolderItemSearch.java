package com.arantius.tivocommander.rpc.request;

import org.codehaus.jackson.JsonNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RecordingFolderItemSearch extends MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  /** Produces an idSequence of shows for the given folder, all if null. */
  public RecordingFolderItemSearch(String folderId) {
    super("recordingFolderItemSearch");

    mergeCommonDetails();
    try {
      mData.put("format", "idSequence");
      if (folderId != null) {
        mData.put("parentRecordingFolderItemId", folderId);
      }
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create RecordingFolderItemSearch request", e);
    }
  }

  /** Given a set of IDs, produces details about the shows. */
  public RecordingFolderItemSearch(JsonNode showIds) {
    super("recordingFolderItemSearch");

    mergeCommonDetails();
    try {
      JSONArray showIdsA = new JSONArray();
      for (JsonNode id : showIds) {
        showIdsA.put(id.getValueAsText());
      }
      mData.put("objectIdAndType", showIdsA);
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create RecordingFolderItemSearch request", e);
    }
  }

  private void mergeCommonDetails() {
    JSONObject details = null;
    try {
      // @formatter:off
      details = new JSONObject("{"
          + "'orderBy': ['startTime'],"
          + "'bodyId': '-',"
          + "'note': ['recordingForChildRecordingId'],"
          + "}");
      // @formatter:on
    } catch (JSONException e) {
      Log.e(LOG_TAG, "merge common failure", e);
    }
    mergeJson(details, mData);
  }
}
