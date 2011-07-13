package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.arantius.tivocommander.rpc.response.RecordingFolderItemListResponse;

public class UiNavigate extends MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  /** Playback for a given item. */
  public UiNavigate(RecordingFolderItemListResponse.RecordingFolderItem item) {
    super("uiNavigate");

    JSONObject details = null;
    try {
      // @formatter:off
      details = new JSONObject("{"
          + "'uri': 'x-tivo:classicui:playback',"
          + "'parameters': {"
            + "'fUseTrioId': 'true',"
            + "'recordingId': '"+item.getRecordingId()+"',"
            + "'fHideBannerOnEnter': 'false'"
            + "}"
          + "}");
      // @formatter:on
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Create UiNavigate request", e);
    }
    mergeJson(details, mData);
  }

}
