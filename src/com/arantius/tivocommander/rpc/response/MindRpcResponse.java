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

package com.arantius.tivocommander.rpc.response;

import org.codehaus.jackson.JsonNode;

public class MindRpcResponse {
  private final JsonNode mBody;
  private final Boolean mIsFinal;
  private final int mRpcId;
  private final String mRespType;

  public MindRpcResponse(Boolean isFinal, int rpcId, JsonNode bodyObj) {
    mBody = bodyObj;
    mIsFinal = isFinal;
    mRpcId = rpcId;
    mRespType = bodyObj.path("type").getTextValue();
  }

  public JsonNode getBody() {
    return mBody;
  }

  public int getRpcId() {
    return mRpcId;
  }

  public String getRespType() {
    return mRespType;
  }

  public Boolean isFinal() {
    return mIsFinal;
  }
}
