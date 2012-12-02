package com.arantius.tivocommander.rpc.request;

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.rpc.MindRpc;

public class RecordingSearch extends MindRpcRequest {
  public RecordingSearch(String recordingId) {
    super("recordingSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    if ("deleted".equals(recordingId)) {
      mDataMap.put("format", "idSequence");
      mDataMap.put("state", new String[] { "deleted" });
    } else {
      mDataMap.put("recordingId", new String[] { recordingId });
    }
    mDataMap.put("levelOfDetail", "high");
  }

  public RecordingSearch(ArrayList<JsonNode> showIds) {
    super("recordingSearch");

    mDataMap.put("objectIdAndType", showIds);
  }
}
