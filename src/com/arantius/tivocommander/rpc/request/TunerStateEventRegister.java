package com.arantius.tivocommander.rpc.request;

public class TunerStateEventRegister extends MindRpcRequest {
  public TunerStateEventRegister() {
    super("tunerStateEventRegister");
    mResponseCount = "multiple";
  }
}
