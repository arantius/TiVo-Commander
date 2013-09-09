package com.arantius.tivocommander.rpc.request;

import com.arantius.tivocommander.rpc.MindRpc;

public class TodoSearch extends MindRpcRequest {
  public TodoSearch() {
    super("recordingSearch");

    mDataMap.put("bodyId", MindRpc.mTivoDevice.tsn);
    mDataMap.put("format", "idSequence");
    mDataMap.put("state", new String[] { "inProgress", "scheduled" });
  }
}
