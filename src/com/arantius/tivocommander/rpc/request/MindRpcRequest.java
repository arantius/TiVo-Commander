package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.arantius.tivocommander.rpc.MindRpc;

public abstract class MindRpcRequest {
  private int mRpcId = 1;
  protected int mSessionId = 0;

  protected String mType;
  protected String mResponseCount = "single";
  protected String mBodyId = "";
  protected JSONObject mData = new JSONObject();

  public MindRpcRequest(String type) {
    setRpcId(MindRpc.getRpcId());
    mSessionId = MindRpc.mSessionId;
    mType = type;

    try {
      mData.put("type", mType);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      Log.e("tivo", "can't put type?", e);
    }
  }

  public String getDataString() {
    return mData.toString();
  }

  public int getRpcId() {
    return mRpcId;
  }

  protected void setRpcId(int mRpcId) {
    this.mRpcId = mRpcId;
  }

  private String join(String glue, String... s) {
    if (s.length == 0) {
      return null;
    }
    StringBuilder out = new StringBuilder();
    out.append(s[0]);
    for (int i = 1; i < s.length; i++) {
      out.append(glue).append(s[i]);
    }
    return out.toString();
  }

  /**
   * Convert the request into a well formatted string for the network.
   *
   * @return String
   */
  @Override
  public String toString() {
    String headers =
        join("\r\n", "Type:request", "RpcId:" + getRpcId(), "SchemaVersion:7",
            "Content-Type:application/json", "RequestType:" + mType,
            "ResponseCount:" + mResponseCount, "BodyId:" + mBodyId,
            "X-ApplicationName:Quicksilver", "X-ApplicationVersion:1.2",
            String.format("X-ApplicationSessionId:0x%x", mSessionId));
    String body = mData.toString() + "\n";
    // "+ 2" is the "\r\n" we'll add next.
    String reqLine =
        String.format("MRPC/2 %d %d", headers.length() + 2, body.length());
    return join("\r\n", reqLine, headers, body);
  }
}
