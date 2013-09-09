package com.arantius.tivocommander.rpc.request;

import java.util.ArrayList;

import com.arantius.tivocommander.rpc.MindRpc;
import com.fasterxml.jackson.databind.JsonNode;

public class RecordingSearch extends MindRpcRequest {
  public RecordingSearch(String recordingId) {
    super("recordingSearch");

    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
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
