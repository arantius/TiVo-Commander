package com.arantius.tivocommander.rpc.response;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class RecordingFolderItemListResponse extends MindRpcResponse {
  public class RecordingFolderItem {
    private final int mFolderItemCount;
    private final String mRecordingId;
    private final String mRecordingFolderId;
    private final String mTitle;
    private final String mType;

    public RecordingFolderItem(JSONObject item) throws JSONException {
      mFolderItemCount = item.optInt("folderItemCount");
      mRecordingId = item.optString("childRecordingId");
      mRecordingFolderId = item.optString("recordingFolderItemId");
      mTitle = item.getString("title");

      JSONObject recording = item.getJSONObject("recordingForChildRecordingId");
      mType = recording.optString("type");
    }

    public int getFolderItemCount() {
      return mFolderItemCount;
    }

    public String getRecordingId() {
      return mRecordingId;
    }

    public String getRecordingFolderId() {
      return mRecordingFolderId;
    }

    public String getTitle() {
      return mTitle;
    }

    public String getType() {
      return mType;
    }
  }

  private static final String LOG_TAG = "tivo_commander";

  private final ArrayList<RecordingFolderItem> mItems =
      new ArrayList<RecordingFolderItem>();

  public RecordingFolderItemListResponse(Boolean isFinal, int rpcId,
      JSONObject bodyObj) {
    super(isFinal, rpcId, bodyObj);

    JSONArray items;
    try {
      items = bodyObj.getJSONArray("recordingFolderItem");
    } catch (JSONException e) {
      Log.e(LOG_TAG, "Grab recordingFolderItem", e);
      return;
    }

    for (int i = 0; i < items.length(); i++) {
      JSONObject item;
      try {
        item = items.getJSONObject(i);
//        Log.i(LOG_TAG, item.toString());
        mItems.add(new RecordingFolderItem(item));
      } catch (JSONException e) {
        Log.e(LOG_TAG, "Grab item", e);
      }
    }
  }

  public ArrayList<RecordingFolderItem> getItems() {
    return mItems;
  }
}
