package com.arantius.tivocommander.rpc.request;

import org.codehaus.jackson.JsonNode;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class UiNavigate extends MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  /** Playback for a given item. */
  public UiNavigate(JsonNode item) {
    super("uiNavigate");

    JSONObject details = null;
    try {
      String recordingId = item.get("childRecordingId").getValueAsText();
      // @formatter:off
      details = new JSONObject("{"
          + "'uri': 'x-tivo:classicui:playback',"
          + "'parameters': {"
            + "'fUseTrioId': 'true',"
            + "'recordingId': '" + recordingId + "',"
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
