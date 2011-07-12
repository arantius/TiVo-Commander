package com.arantius.tivocommander.rpc.request;

import org.json.JSONException;

import android.util.Log;

public class KeyEventSend extends MindRpcRequest {
  public KeyEventSend(String key) {
    super("keyEventSend");

    try {
      mData.put("event", key);
    } catch (JSONException e) {
      Log.e("tivo", "", e);
    }
  }

  public KeyEventSend(char letter) {
    super("keyEventSend");

    try {
      mData.put("event", "ascii");
      mData.put("value", letter);
    } catch (JSONException e) {
      Log.e("tivo", "", e);
    }
  }
}
