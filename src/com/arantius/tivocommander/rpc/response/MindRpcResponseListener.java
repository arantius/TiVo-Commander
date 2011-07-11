package com.arantius.tivocommander.rpc.response;

public interface MindRpcResponseListener {
  /**
   * Handle the response, if appropriate; return true if handled, false if not.
   * 
   * @param response The new response object being dispatched.
   */
  public void onResponse(MindRpcResponse response);
}
