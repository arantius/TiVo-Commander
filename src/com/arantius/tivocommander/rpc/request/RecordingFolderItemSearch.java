package com.arantius.tivocommander.rpc.request;

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

  /** Given a JSONArray of ids, produces details about the shows. */
  public RecordingFolderItemSearch(JSONArray showIds) {
    super("recordingFolderItemSearch");

    mergeCommonDetails();
    try {
      mData.put("objectIdAndType", showIds);
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
