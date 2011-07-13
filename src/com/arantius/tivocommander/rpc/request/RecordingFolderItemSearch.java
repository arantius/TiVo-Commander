package com.arantius.tivocommander.rpc.request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RecordingFolderItemSearch extends MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  /** Produces an idSequence of shows. */
  public RecordingFolderItemSearch() {
    super("recordingFolderItemSearch");

    JSONObject details = null;
    try {
      // @formatter:off
      details = new JSONObject("{"
          + "'orderBy': ['startTime'],"
          + "'bodyId': '-',"
          + "'format': 'idSequence',"
          + "'note': ['recordingForChildRecordingId'],"
          + "}");
      // @formatter:on
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create RecordingFolderItemSearch request", e);
    }
    mergeJson(details, mData);
  }

  /** Given a JSONArray of ids, produces details about the shows. */
  public RecordingFolderItemSearch(JSONArray showIds) {
    super("recordingFolderItemSearch");

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
      Log.e(LOG_TAG, "", e);
    }
    mergeJson(details, mData);

    try {
      mData.put("objectIdAndType", showIds);
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create RecordingFolderItemSearch request", e);
    }
  }
}
