package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class RecordingFolderItemEmpty extends MindRpcRequest {
  public RecordingFolderItemEmpty(String recordingFolderItemId) {
    super("recordingFolderItemEmpty");
    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
    mDataMap.put("recordingFolderItemId", recordingFolderItemId);
  }
}
