package com.arantius.tivocommander.rpc.request;

public class RecordingSearch extends MindRpcRequest {
  public RecordingSearch(String recordingId) {
    super("recordingSearch");

    mDataMap.put("bodyId", "-");
    mDataMap.put("recordingId", new String[] { recordingId });
    mDataMap.put("levelOfDetail", "high");
  }
}
