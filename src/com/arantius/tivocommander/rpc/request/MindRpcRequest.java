/*
TiVo Commander allows control of a TiVo Premiere device.
Copyright (C) 2011  Anthony Lieuallen (arantius@gmail.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.arantius.tivocommander.rpc.request;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import com.arantius.tivocommander.Utils;
import com.arantius.tivocommander.rpc.MindRpc;

public abstract class MindRpcRequest {
  private String mReqType;

  protected Map<String, Object> mDataMap = new HashMap<String, Object>();
  protected String mResponseCount = "single";
  protected int mRpcId;
  protected int mSessionId = 0;

  public MindRpcRequest(String type) {
    mRpcId = MindRpc.getRpcId();
    mSessionId = MindRpc.getSessionId();
    setReqType(type);
  }

  public Map<String, Object> getDataMap() {
    return mDataMap;
  }

  public String getDataString() {
    String data = Utils.stringifyToJson(mDataMap);
    if (data == null) {
      Utils.logError("Stringify failure; request body");
    }
    return data;
  }

  public String getReqType() {
    return mReqType;
  }

  public int getRpcId() {
    return mRpcId;
  }

  public void setReqType(String type) {
    mReqType = type;
    mDataMap.put("type", mReqType);
  }

  /**
   * Convert the request into a well formatted byte array for the network.
   * @throws UnsupportedEncodingException
   */
  public byte[] getBytes() throws UnsupportedEncodingException {
    // @formatter:off
    String headers = Utils.join("\r\n",
        "Type: request",
        "RpcId: " + getRpcId(),
        "SchemaVersion: 7",
        "Content-Type: application/json",
        "RequestType: " + mReqType,
        "ResponseCount: " + mResponseCount,
        "BodyId: " + MindRpc.mBodyId,
        "X-ApplicationName: Quicksilver ",
        "X-ApplicationVersion: 1.2 ",
        String.format("X-ApplicationSessionId: 0x%x", mSessionId));
    // @formatter:on
    String body = getDataString();

    // NOTE: The lengths here must be the length in *bytes*, not characters!
    // Thus all the .getBytes() conversions to find the proper lengths.
    // "+ 2" is the "\r\n" we'll add next.
    String reqLine =
        String.format("MRPC/2 %d %d", headers.getBytes("UTF-8").length + 2,
            body.getBytes("UTF-8").length);
    String request = Utils.join("\r\n", reqLine, headers, body);
    byte[] requestBytes = request.getBytes("UTF-8");
    return requestBytes;
  }
}
