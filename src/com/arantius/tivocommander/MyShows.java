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

package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.SimpleAdapter;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class MyShows extends ListActivity {
  private final static int EXPLORE_INTENT_ID = 1;

  private final MindRpcResponseListener mDetailCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mItems = response.getBody().path("recordingFolderItem");

          for (int i = 0; i < mItems.size(); i++) {
            final JsonNode item = mItems.path(i);
            HashMap<String, Object> listItem = new HashMap<String, Object>();

            String title = item.path("title").getTextValue();
            if ('"' == title.charAt(0)
                && '"' == title.charAt(title.length() - 1)) {
              title = title.substring(1, title.length() - 1);
            }
            listItem.put("folder_num", "");
            listItem.put("icon", getIconForItem(item));
            listItem.put("title", title);
            Integer folderItemCount =
                item.path("folderItemCount").getIntValue();
            if (folderItemCount > 0) {
              listItem.put("folder_num", folderItemCount.toString());
            }
            mListItems.add(listItem);
          }

          mListAdapter.notifyDataSetChanged();
          setProgressBarIndeterminateVisibility(false);
        }
      };
  private String mFolderId;
  private final MindRpcResponseListener mIdSequenceCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode ids = response.getBody().findValue("objectIdAndType");
          MindRpc.addRequest(new RecordingFolderItemSearch(ids),
              mDetailCallback);
          // TODO: Incremental detail loading.
        }
      };
  private JsonNode mItems;
  private SimpleAdapter mListAdapter;
  private final List<HashMap<String, Object>> mListItems =
      new ArrayList<HashMap<String, Object>>();
  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode item = mItems.path(position);
          JsonNode countNode = item.path("folderItemCount");
          if (countNode != null && countNode.getValueAsInt() > 0) {
            // Navigate to 'my shows' for this folder.
            Intent intent = new Intent(MyShows.this, MyShows.class);
            intent.putExtra("folderId", item.path("recordingFolderItemId")
                .getValueAsText());
            intent.putExtra("folderName", item.path("title").getValueAsText());
            startActivity(intent);
          } else {
            JsonNode recording = item.path("recordingForChildRecordingId");

            Intent intent = new Intent(MyShows.this, ExploreTabs.class);
            intent.putExtra("contentId", recording.path("contentId")
                .getTextValue());
            intent.putExtra("collectionId", recording.path("collectionId")
                .getTextValue());
            intent.putExtra("recordingId", item.path("childRecordingId")
                .getTextValue());
            startActivityForResult(intent, EXPLORE_INTENT_ID);
          }
        }
      };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    // TODO: Sorting.
    // TODO: Show disk usage.
    // TODO: Show date recorded.

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list);

    mListAdapter =
        new SimpleAdapter(MyShows.this, mListItems, R.layout.item_my_shows,
            new String[] { "folder_num", "icon", "title" }, new int[] {
                R.id.folder_num, R.id.show_icon, R.id.show_title });
    getListView().setAdapter(mListAdapter);
    getListView().setOnItemClickListener(mOnClickListener);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mFolderId = bundle.getString("folderId");
      setTitle("TiVo Commander - " + bundle.getString("folderName"));
    } else {
      mFolderId = null;
      setTitle("TiVo Commander - My Shows");
    }

    startRequest();
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }

  private void startRequest() {
    setProgressBarIndeterminateVisibility(true);

    // Clear any possible old data.
    mListItems.clear();
    mListAdapter.notifyDataSetChanged();
    // Get new data.
    MindRpc.addRequest(new RecordingFolderItemSearch(mFolderId),
        mIdSequenceCallback);
  }

  protected final int getIconForItem(JsonNode item) {
    String folderTransportType =
        item.path("folderTransportType").path(0).getTextValue();
    if ("mrv".equals(folderTransportType)) {
      return R.drawable.folder_downloading;
    } else if ("stream".equals(folderTransportType)) {
      return R.drawable.folder_recording;
    }

    if (item.has("folderItemCount")) {
      if ("wishlist".equals(item.path("folderType").getTextValue())) {
        return R.drawable.folder_wishlist;
      } else {
        return R.drawable.folder;
      }
    }

    if (item.has("recordingStatusType")) {
      String recordingStatus = item.path("recordingStatusType").getTextValue();
      if ("expired".equals(recordingStatus)) {
        return R.drawable.recording_expired;
      } else if ("expiresSoon".equals(recordingStatus)) {
        return R.drawable.recording_expiressoon;
      } else if ("inProgressDownload".equals(recordingStatus)) {
        return R.drawable.recording_downloading;
      } else if ("inProgressRecording".equals(recordingStatus)) {
        return R.drawable.recording_recording;
      } else if ("keepForever".equals(recordingStatus)) {
        return R.drawable.recording_keep;
      } else if ("suggestion".equals(recordingStatus)) {
        return R.drawable.recording_suggestion;
      } else if ("wishlist".equals(recordingStatus)) {
        return R.drawable.recording_wishlist;
      }
    }

    if ("recording".equals(item.path("recordingForChildRecordingId")
        .path("type").getTextValue())) {
      return R.drawable.recording;
    }

    return R.drawable.blank;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }

    if (EXPLORE_INTENT_ID == requestCode) {
      if (data.getBooleanExtra("refresh", false)) {
        Utils.log("do refresh");
        startRequest();
      }
    }
  }
}
