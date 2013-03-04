/*
DVR Commander for TiVo allows control of a TiVo Premiere device.
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

import com.arantius.tivocommander.Utils;

public class CancelRpc extends MindRpcRequest {
  protected Integer mCancelRpcId;

  public CancelRpc(int rpcId) {
    super("cancel");
    mCancelRpcId = rpcId;
  }

  @Override
  public byte[] getBytes() throws UnsupportedEncodingException {
    // @formatter:off
    String headers = Utils.join("\r\n",
        "Type: cancel",
        "RpcId: " + mCancelRpcId.toString(),
        "SchemaVersion:7");
    // @formatter:on
    // "+ 2" is the "\r\n" we'll add next.
    String reqLine =
        String.format("MRPC/2 %d 0", headers.getBytes("UTF-8").length + 2);
    String request =
        Utils.join("\r\n", reqLine, headers, "");
    byte[] requestBytes = request.getBytes("UTF-8");
    return requestBytes;
  }
}
