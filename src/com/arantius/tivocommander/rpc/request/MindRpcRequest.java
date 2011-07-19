package com.arantius.tivocommander.rpc.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

import com.arantius.tivocommander.rpc.MindRpc;

public abstract class MindRpcRequest {
  private static final String LOG_TAG = "tivo_commander";
  private static final ObjectMapper mMapper = new ObjectMapper();
  private final int mRpcId;

  protected String mBodyId = "";
  protected Map<String, Object> mDataMap = new HashMap<String, Object>();
  protected String mResponseCount = "single";
  protected int mSessionId = 0;
  protected String mType;

  public MindRpcRequest(String type) {
    mRpcId = MindRpc.getRpcId();
    mSessionId = MindRpc.getSessionId();
    mType = type;

    mDataMap.put("type", mType);
  }

  public String getDataString() {
    try {
      return mMapper.writeValueAsString(mDataMap);
    } catch (JsonGenerationException e) {
      Log.e(LOG_TAG, "Stringify response body", e);
    } catch (JsonMappingException e) {
      Log.e(LOG_TAG, "Stringify response body", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "Stringify response body", e);
    }
    return null;
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
    String headers = join("\r\n",
        "Type: request",
        "RpcId: " + getRpcId(),
        "SchemaVersion:7",
        "Content-Type: application/json",
        "RequestType: " + mType,
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
    return join("\r\n", reqLine, headers, body);
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
}
