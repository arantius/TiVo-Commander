package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class RecordingSearch extends MindRpcRequest {
  public RecordingSearch(String recordingId) {
    super("recordingSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("recordingId", new String[] { recordingId });
    mDataMap.put("levelOfDetail", "high");
  }
}
