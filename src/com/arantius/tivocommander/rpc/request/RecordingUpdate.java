package com.arantius.tivocommander.rpc.request;


public class RecordingUpdate extends MindRpcRequest {
  public RecordingUpdate(String recordingId, String state) {
    super("recordingUpdate");

    mDataMap.put("bodyId", "-");
    mDataMap.put("recordingId", new String[] { recordingId });
    mDataMap.put("state", state);
  }
}
