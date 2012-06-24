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

package com.arantius.tivocommander.rpc.response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.JsonNode;

import com.arantius.tivocommander.Utils;

public class MindRpcResponseFactory {
  public MindRpcResponse create(byte[] headers, byte[] body) {
    Boolean isFinal = true;
    int rpcId = 0;

    String line;
    BufferedReader headerReader =
        new BufferedReader(new StringReader(new String(headers)));
    try {
      while ((line = headerReader.readLine()) != null) {
        if (line.length() > 9 && "IsFinal:".equals(line.substring(0, 8))) {
          isFinal = "true".equals(line.substring(9));
        } else if (line.length() > 7 && "RpcId:".equals(line.substring(0, 6))) {
          rpcId = Integer.parseInt(line.substring(7));
        }
      }
    } catch (IOException e) {
    }

    String bodyStr = new String(body);
    JsonNode bodyObj = Utils.parseJson(bodyStr);
    if (bodyObj == null) {
      Utils.logError("Parse failure; response body");
      return null;
    } else {
      if (bodyObj.path("type").getValueAsText().equals("error")) {
        Utils.logError("Response type is error! " + bodyStr);
      }

      return new MindRpcResponse(isFinal, rpcId, bodyObj);
    }
  }
}
