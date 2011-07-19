package com.arantius.tivocommander.rpc.response;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.IOException;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import android.util.Log;

public class MindRpcResponseFactory {
  private static final String LOG_TAG = "tivo_commander";
  private static final ObjectMapper mJsonParser = new ObjectMapper();

  public MindRpcResponse create(char[] headers, char[] body) {
    Boolean isFinal = true;
    int rpcId = 0;

    String line;
    BufferedReader headerReader =
        new BufferedReader(new CharArrayReader(headers));
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
    JsonNode bodyObj = null;
    try {
      bodyObj = mJsonParser.readValue(bodyStr, JsonNode.class);
    } catch (JsonMappingException e) {
      Log.e(LOG_TAG, "Parse response body", e);
      return null;
    } catch (JsonParseException e) {
      Log.e(LOG_TAG, "Parse response body", e);
      return null;
    } catch (IOException e) {
      Log.e(LOG_TAG, "Parse response body", e);
      return null;
    }

    if (bodyObj.get("type").getValueAsText().equals("error")) {
      Log.e(LOG_TAG, "Response type is error! " + bodyStr);
    }

    return new MindRpcResponse(isFinal, rpcId, bodyObj);
  }
}
