package com.arantius.tivocommander.rpc.request;

public class VideoPlaybackInfoEventRegister extends MindRpcRequest {
  public VideoPlaybackInfoEventRegister() {
    super("videoPlaybackInfoEventRegister");
    mResponseCount = "multiple";
    mDataMap.put("throttleDelay", 1000);
  }

  public VideoPlaybackInfoEventRegister(int delay) {
    super("videoPlaybackInfoEventRegister");
    mResponseCount = "multiple";
    mDataMap.put("throttleDelay", delay);
  }
}
