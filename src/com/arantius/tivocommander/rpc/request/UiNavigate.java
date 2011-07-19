package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonNode;

public class UiNavigate extends MindRpcRequest {
  /** Playback for a given item. */
  public UiNavigate(JsonNode item) {
    super("uiNavigate");

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("fUseTrioId", "true");
    parameters.put("fHideBannerOnEnter", "false");
    parameters.put("recordingId", item.get("childRecordingId"));

    mDataMap.put("uri", "x-tivo:classicui:playback");
    mDataMap.put("parameters", parameters);
  }
}
