package com.arantius.tivocommander.rpc.request;


public class KeyEventSend extends MindRpcRequest {
  public KeyEventSend(char letter) {
    super("keyEventSend");

    mDataMap.put("event", "ascii");
    mDataMap.put("value", (int) letter);
  }

  public KeyEventSend(String key) {
    super("keyEventSend");

    mDataMap.put("event", key);
  }
}
