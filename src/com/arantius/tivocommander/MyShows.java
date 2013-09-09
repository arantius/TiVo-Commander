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

package com.arantius.tivocommander;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.BodyConfigSearch;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemEmpty;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class MyShows extends ShowList {
  private MindRpcResponseListener mBodyConfigCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setProgressIndicator(-1);

          ProgressBar mMeter = (ProgressBar) findViewById(R.id.meter);
          TextView mMeterText = (TextView) findViewById(R.id.meter_text);
          JsonNode bodyConfig = response.getBody().path("bodyConfig").path(0);
          int used = bodyConfig.path("userDiskUsed").asInt();
          int size = bodyConfig.path("userDiskSize").asInt();

          mMeter.setMax(size);
          mMeter.setProgress(used);

          mMeterText.setText(String.format("%d%% Disk Used",
              (int) ((double) 100 * used / size)));
        }
      };

  public void doSort(View v) {
    ArrayAdapter<String> orderAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            mOrderLabels);
    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Order?");
    dialogBuilder.setAdapter(orderAdapter,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int position) {
            mOrderBy = mOrderValues[position];
            startRequest();
          }
        });
    AlertDialog dialog = dialogBuilder.create();
    dialog.show();
  }

  protected void folderDelete(JsonNode folder) {
    final String folderId = folder.path("recordingFolderItemId").asText();
    RecordingFolderItemEmpty req = new RecordingFolderItemEmpty(folderId);
    MindRpc.addRequest(req, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        startRequest();
      }
    });
  }

  protected void folderPlay(final JsonNode folder) {
    Utils.log("Play folder:");
    setProgressIndicator(1);
    final String folderId = folder.path("recordingFolderItemId").asText();
    final RecordingFolderItemSearch req =
        new RecordingFolderItemSearch(folderId, mOrderBy);
    MindRpc.addRequest(req, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        final RecordingFolderItemSearch req =
            new RecordingFolderItemSearch(
                response.getBody().path("objectIdAndType"), mOrderBy);
        MindRpc.addRequest(req, new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);

            // Gather recordings in reverse order (by always adding to index
            // zero), reversing the reverse-chronological order we got.
            ArrayList<String> recordingIds = new ArrayList<String>();
            for (JsonNode item : response.getBody().path("recordingFolderItem")) {
              recordingIds.add(0, item.path("childRecordingId").asText());
            }

            UiNavigate req =
                new UiNavigate(recordingIds, folder.path("title").asText());
            MindRpc.addRequest(req, null);

            Intent intent = new Intent(MyShows.this, NowShowing.class);
            startActivity(intent);
          }
        });
      }
    });
  }

  protected int getIconForItem(JsonNode item) {
    String folderTransportType =
        item.path("folderTransportType").path(0).asText();
    if ("mrv".equals(folderTransportType)) {
      return R.drawable.folder_downloading;
    } else if ("stream".equals(folderTransportType)) {
      return R.drawable.folder_recording;
    } else if ("deletedFolder".equals(folderTransportType)) {
      return R.drawable.folder;
    }

    if (item.has("folderItemCount")) {
      if ("wishlist".equals(item.path("folderType").asText())) {
        return R.drawable.folder_wishlist;
      } else {
        return R.drawable.folder;
      }
    }

    if (item.has("recordingStatusType")) {
      String recordingStatus = item.path("recordingStatusType").asText();
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
        .path("type").asText())) {
      return R.drawable.recording;
    }

    if ("deleted".equals(item.path("state").asText())) {
      return R.drawable.recording_deleted;
    }

    return R.drawable.blank;
  }

  protected Pair<ArrayList<String>, ArrayList<Integer>> getLongPressChoices(
      JsonNode item) {
    final ArrayList<String> choices = new ArrayList<String>();
    final ArrayList<Integer> actions = new ArrayList<Integer>();

    final String folderType = item.path("folderType").asText();
    if ("series".equals(folderType) || "wishlist".equals(folderType)) {
      // For season pass and/or wish list folders.
      choices.add(getResources().getString(R.string.watch_folder));
      actions.add(R.string.watch_folder);
      choices.add(getResources().getString(R.string.delete_folder));
      actions.add(R.string.delete_folder);
    } else if (!"".equals(folderType)) {
      // Other folders not supported.
      return null;
    } else {
      // For individual recordings.
      JsonNode recording = item.path("recordingForChildRecordingId");
      if ("deleted".equals(mFolderId)) {
        choices.add(getResources().getString(R.string.undelete));
        actions.add(R.string.undelete);
      } else {
        choices.add(getResources().getString(R.string.watch_now));
        actions.add(R.string.watch_now);
        if ("inProgress" == recording.path("state").asText()) {
          choices.add(getResources().getString(R.string.stop_recording));
          actions.add(R.string.stop_recording);
          choices.add(getResources()
              .getString(R.string.stop_recording_and_delete));
          actions.add(R.string.stop_recording_and_delete);
        } else {
          choices.add(getResources().getString(R.string.delete));
          actions.add(R.string.delete);
        }
      }
    }

    return Pair.create(choices, actions);
  }

  protected JsonNode getRecordingFromItem(JsonNode item) {
    if ("deleted".equals(mFolderId)) {
      // In deleted mode, we directly fetch recordings.
      return item;
    } else {
      // Otherwise we've got recordings wrapped in folders.
      return item.path("recordingForChildRecordingId");
    }
  }

  @Override
  public void onClick(DialogInterface dialog, int position) {
    final Pair<ArrayList<String>, ArrayList<Integer>> choices =
        getLongPressChoices(mLongPressItem);
    Integer action = choices.second.get(position);

    switch (action) {
    case R.string.delete_folder:
      folderDelete(mLongPressItem);
      break;
    case R.string.watch_folder:
      folderPlay(mLongPressItem);
      return;
    default:
      super.onClick(dialog, position);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    if (MindRpc.init(this, bundle)) {
      return;
    }

    if (bundle != null) {
      mFolderId = bundle.getString("folderId");
      setTitle(bundle.getString("folderName"));
    } else {
      mFolderId = null;
      setTitle("My Shows");
    }

    Utils.log(String.format("MyShows: folderId:%s", mFolderId));

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list_my_shows);

    if (mFolderId != null) {
      findViewById(R.id.sort_button).setVisibility(View.GONE);
    }

    mListAdapter = new ShowsAdapter(this);
    final ListView lv = getListView();
    lv.setAdapter(mListAdapter);
    lv.setOnItemClickListener(mOnClickListener);
    lv.setLongClickable(true);
    lv.setOnItemLongClickListener(this);

    mDetailCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);

            String itemId = "recordingFolderItem";
            if ("deleted".equals(mFolderId)) {
              itemId = "recording";
            }
            final JsonNode items = response.getBody().path(itemId);

            ArrayList<Integer> slotMap =
                mRequestSlotMap.get(response.getRpcId());

            MindRpc.saveBodyId(
                items.path(0).path("bodyId").asText(), MyShows.this);

            for (int i = 0; i < items.size(); i++) {
              int pos = slotMap.get(i);
              JsonNode item = items.get(i);
              mShowData.set(pos, item);
              mShowStatus.set(pos, ShowStatus.LOADED);
            }

            mRequestSlotMap.remove(response.getRpcId());
            mListAdapter.notifyDataSetChanged();
          }
        };

    mIdSequenceCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            JsonNode body = response.getBody();
            if ("error".equals(body.path("status").asText())
                || !body.has("objectIdAndType")) {
              Utils.log("Handling mIdSequenceCallback error response by "
                  + "finishWithRefresh()");
              finishWithRefresh();
              return;
            }

            setProgressIndicator(-1);

            mShowIds = (ArrayNode) body.findValue("objectIdAndType");
            if (mFolderId == null) {
              mShowIds.add("deleted");
            }

            if (mShowIds != null) {
              // e.g. "Suggestions" can be present, but empty!
              for (int i = 0; i < mShowIds.size(); i++) {
                mShowData.add(null);
                mShowStatus.add(ShowStatus.MISSING);
              }
            }
            mListAdapter.notifyDataSetChanged();
          }
        };

    startRequest();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:MyShows");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:MyShows");
    MindRpc.init(this, null);
  }

  protected void startRequest() {
    mShowData.clear();
    mShowStatus.clear();
    mListAdapter.notifyDataSetChanged();
    if ("deleted".equals(mFolderId)) {
      MindRpc.addRequest(new RecordingSearch(mFolderId), mIdSequenceCallback);
    } else {
      MindRpc.addRequest(new RecordingFolderItemSearch(mFolderId, mOrderBy),
          mIdSequenceCallback);
    }
    setProgressIndicator(1);
    MindRpc.addRequest(new BodyConfigSearch(), mBodyConfigCallback);
    setProgressIndicator(1);
  }
}
