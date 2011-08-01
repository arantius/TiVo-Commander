package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.MindRpc;

public abstract class MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";

  protected String mBodyId = "";
  protected Map<String, Object> mDataMap = new HashMap<String, Object>();
  protected String mReqType;
  protected String mResponseCount = "single";
  protected int mRpcId;
  protected int mSessionId = 0;
  protected final String mType = "request";

  public MindRpcRequest(String type) {
    mRpcId = MindRpc.getRpcId();
    mSessionId = MindRpc.getSessionId();
    mReqType = type;

    mDataMap.put("type", mReqType);
  }

  public Map<String, Object> getDataMap() {
    return mDataMap;
  }

  public String getDataString() {
    String data = Utils.stringifyToJson(mDataMap);
    if (data == null) {
      Log.e(LOG_TAG, "Stringify failure; request body");
    }
    return data;
  }

  public String getReqType() {
    return mReqType;
  }

  public int getRpcId() {
    return mRpcId;
  }

  /**
   * Convert the request into a well formatted string for the network.
   *
   * @return String
   */
  @Override
  public String toString() {
    // @formatter:off
    String headers = Utils.join("\r\n",
        "Type: " + mType,
        "RpcId: " + getRpcId(),
        "SchemaVersion:7",
        "Content-Type: application/json",
        "RequestType: " + mReqType,
        "ResponseCount: " + mResponseCount,
        "BodyId: " + mBodyId,
        "X-ApplicationName:Quicksilver ",
        "X-ApplicationVersion:1.2 ",
        String.format("X-ApplicationSessionId: 0x%x", mSessionId));
    // @formatter:on
    String body = getDataString();
    // "+ 2" is the "\r\n" we'll add next.
    String reqLine =
        String.format("MRPC/2 %d %d", headers.length() + 2, body.length());
    return Utils.join("\r\n", reqLine, headers, body);
  }
}
