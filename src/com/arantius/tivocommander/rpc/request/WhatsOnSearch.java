package com.arantius.tivocommander.rpc.request;

public class WhatsOnSearch extends MindRpcRequest {
  public WhatsOnSearch() {
    super("whatsOnSearch");
    mResponseCount = "multiple";
  }
}
