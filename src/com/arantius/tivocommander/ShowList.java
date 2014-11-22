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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.request.RecordingUpdate;
import com.arantius.tivocommander.rpc.request.TodoRecordingSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public abstract class ShowList extends ListActivity implements
    OnItemLongClickListener, DialogInterface.OnClickListener {
  protected class ShowsAdapter extends ArrayAdapter<JsonNode> {
    protected Context mContext;

    public ShowsAdapter(Context context) {
      super(context, 0, mShowData);
      mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (mShowStatus.get(position) == ShowStatus.MISSING) {
        // If the show for this position is missing, fetch it and more, if they
        // exist, up to a limit of MAX_SHOW_REQUEST_BATCH.
        ArrayList<JsonNode> showIds = new ArrayList<JsonNode>();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        int i = position;
        while (i < mShowStatus.size()) {
          if (mShowStatus.get(i) == ShowStatus.MISSING) {
            JsonNode showId = mShowIds.get(i);
            if ("deleted".equals(showId.asText())) {
              mShowData.set(i, mDeletedItem);
              mShowStatus.set(i, ShowStatus.LOADED);
            } else {
              showIds.add(showId);
              slots.add(i);
              mShowStatus.set(i, ShowStatus.LOADING);
              if (showIds.size() >= MAX_SHOW_REQUEST_BATCH) {
                break;
              }
            }
          }
          i++;
        }

        // We could rarely have no shows, if the "recently deleted" item
        // which we don't fetch falls right on the border.
        if (showIds.size() > 0) {
          MindRpcRequest req;
          if (mContext instanceof ToDo) {
            req = new TodoRecordingSearch(showIds, mOrderBy);
          } else if (mContext instanceof MyShows) {
            if ("deleted".equals(mFolderId)) {
              req = new RecordingSearch(showIds);
            } else {
              req = new RecordingFolderItemSearch(showIds, mOrderBy);
            }
          } else {
            Utils.logError("Unsupported context!");
            return null;
          }
          mRequestSlotMap.put(req.getRpcId(), slots);
          MindRpc.addRequest(req, mDetailCallback);
          setProgressIndicator(1);
        }
      }

      LayoutInflater vi =
          (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      if (mShowStatus.get(position) == ShowStatus.LOADED) {
        // If this item is available, display it.
        v = vi.inflate(R.layout.item_my_shows, parent, false);
        final JsonNode item = mShowData.get(position);
        final JsonNode recording = getRecordingFromItem(item);

        ((TextView) v.findViewById(R.id.show_title)).setText(
            Utils.stripQuotes(item.path("title").asText()));

        Integer folderItemCount = item.path("folderItemCount").asInt();
        ((TextView) v.findViewById(R.id.folder_num))
            .setText(folderItemCount > 0 ? folderItemCount.toString() : "");

        String channelStr = "";
        JsonNode channel = recording.path("channel");
        if (folderItemCount == 0 && !channel.isMissingNode()) {
          channelStr =
              String.format("%s %s", channel.path("channelNumber")
                  .asText(), channel.path("callSign").asText());
        }
        ((TextView) v.findViewById(R.id.show_channel)).setText(channelStr);

        String startTimeStr = item.path("startTime").asText();
        if ("".equals(startTimeStr)) {
          // Rarely the time is only on the recording, not the item.
          startTimeStr = recording.path("startTime").asText();
        }

        if ("".equals(startTimeStr)
            || "1970".equals(startTimeStr.substring(0, 4))) {
          v.findViewById(R.id.show_time).setVisibility(View.GONE);
        } else {
          Date startTime =
              Utils.parseDateTimeStr(startTimeStr);
          String timeFormat = "EEE M/d";
          if (mContext instanceof ToDo) {
            timeFormat += " h:mm aa";
          }
          SimpleDateFormat dateFormatter =
              new SimpleDateFormat(timeFormat, Locale.US);
          String timeStr = dateFormatter.format(startTime);
          ((TextView) v.findViewById(R.id.show_time)).setText(timeStr);
        }

        final int iconId = getIconForItem(item);
        ((ImageView) v.findViewById(R.id.show_icon))
            .setImageDrawable(getResources().getDrawable(iconId));
      } else {
        // Otherwise give a loading indicator.
        v = vi.inflate(R.layout.progress, parent, false);
      }

      return v;
    }
  }

  protected enum ShowStatus {
    LOADED, LOADING, MISSING;
  }

  protected final static int EXPECT_REFRESH_INTENT_ID = 1;
  protected final static int MAX_SHOW_REQUEST_BATCH = 5;
  protected final JsonNode mDeletedItem = Utils
      .parseJson("{\"folderTransportType\":[\"deletedFolder\"]"
          + ",\"recordingFolderItemId\":\"deleted\""
          + ",\"title\":\"Recently Deleted\"}");
  protected MindRpcResponseListener mDetailCallback;
  protected String mFolderId;
  protected MindRpcResponseListener mIdSequenceCallback;
  protected ShowsAdapter mListAdapter;
  protected int mLongPressIndex;
  protected JsonNode mLongPressItem;
  protected String mOrderBy = "startTime";
  protected final String[] mOrderLabels = new String[] { "Date", "A-Z" };
  protected final String[] mOrderValues = new String[] { "startTime", "title" };
  protected int mRequestCount = 0;
  protected final SparseArray<ArrayList<Integer>> mRequestSlotMap =
      new SparseArray<ArrayList<Integer>>();
  protected final ArrayList<JsonNode> mShowData = new ArrayList<JsonNode>();
  protected ArrayNode mShowIds;
  protected final ArrayList<ShowStatus> mShowStatus =
      new ArrayList<ShowStatus>();

  protected final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          final JsonNode item = mShowData.get(position);
          if (item == null) {
            return;
          }

          final JsonNode countNode = item.path("folderItemCount");
          if (mDeletedItem == item
              || (countNode != null && countNode.asInt() > 0)) {
            // Navigate to 'my shows' for this folder.
            Intent intent = new Intent(ShowList.this, MyShows.class);
            intent.putExtra("folderId", item.path("recordingFolderItemId")
                .asText());
            intent.putExtra("folderName", item.path("title").asText());
            startActivityForResult(intent, EXPECT_REFRESH_INTENT_ID);
          } else {
            final JsonNode recording = getRecordingFromItem(item);

            Intent intent = new Intent(ShowList.this, ExploreTabs.class);
            intent.putExtra("contentId", recording.path("contentId")
                .asText());
            intent.putExtra("collectionId", recording.path("collectionId")
                .asText());

            // Regular / deleted recordings IDs are differently located.
            if (item.has("childRecordingId")) {
              intent.putExtra("recordingId", item.path("childRecordingId")
                  .asText());
            } else if (item.has("recordingId")) {
              intent.putExtra("recordingId", item.path("recordingId")
                  .asText());
            }

            startActivityForResult(intent, EXPECT_REFRESH_INTENT_ID);
          }
        }
      };

  protected abstract int getIconForItem(JsonNode item);

  protected abstract Pair<ArrayList<String>, ArrayList<Integer>> getLongPressChoices(
      JsonNode item);

  protected abstract JsonNode getRecordingFromItem(JsonNode item);

  protected abstract void startRequest();

  protected void finishWithRefresh() {
    setRefreshResult();
    finish();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }

    if (EXPECT_REFRESH_INTENT_ID == requestCode) {
      if (data.getBooleanExtra("refresh", false)) {
        setRefreshResult();
        if (mShowData.size() == 1) {
          // We deleted the last show! Go up a level.
          finishWithRefresh();
        } else {
          // Load the list of remaining shows.
          startRequest();
          notifyParentToRefresh();
        }
      }
    }
  };

  public void onClick(DialogInterface dialog, int position) {
    final Pair<ArrayList<String>, ArrayList<Integer>> choices =
        getLongPressChoices(mLongPressItem);
    Integer action = choices.second.get(position);

    String recordingId = mLongPressItem.path("recordingId").asText();

    MindRpcRequest req = null;
    final MindRpcResponseListener reqListener =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);
            // Now that it's probably changed, re-load the list.
            startRequest();
          }
        };
    final MindRpcResponseListener removeListener =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);
            mShowData.remove(mLongPressIndex);
            mShowIds.remove(mLongPressIndex);
            mShowStatus.remove(mLongPressIndex);
            mListAdapter.notifyDataSetChanged();
          }
        };
    MindRpcResponseListener listener = reqListener;

    switch (action) {
    case R.string.delete:
    case R.string.stop_recording_and_delete:
      req = new RecordingUpdate(recordingId, "deleted");
      listener = removeListener;
      notifyParentToRefresh();
      break;
    case R.string.dont_record:
      req = new RecordingUpdate(recordingId, "cancelled");
      listener = removeListener;
      notifyParentToRefresh();
      break;
    case R.string.stop_recording:
    case R.string.undelete:
      req = new RecordingUpdate(recordingId, "complete");
      notifyParentToRefresh();
      break;
    case R.string.watch_now:
      req = new UiNavigate(recordingId);
      listener =
          new MindRpcResponseListener() {
            public void onResponse(MindRpcResponse response) {
              setProgressIndicator(-1);
              Intent intent = new Intent(ShowList.this, NowShowing.class);
              startActivity(intent);
            }
          };
      break;
    }

    setProgressIndicator(1);
    MindRpc.addRequest(req, listener);
  }

  protected void notifyParentToRefresh() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("refresh", true);
    setResult(Activity.RESULT_OK, resultIntent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Utils.createFullOptionsMenu(menu, this);
    return true;
  }

  public boolean onItemLongClick(AdapterView<?> parent, View view,
      int position, long id) {
    if (position > mShowData.size()) {
      return false;
    }
    if (mShowStatus.get(position) != ShowStatus.LOADED) {
      return false;
    }

    mLongPressIndex = position;
    mLongPressItem = mShowData.get(position);
    if (mLongPressItem.has("recordingForChildRecordingId")
        && !mLongPressItem.has("folderType")) {
      mLongPressItem = mLongPressItem.path("recordingForChildRecordingId");
    }

    final Pair<ArrayList<String>, ArrayList<Integer>> choices =
        getLongPressChoices(mLongPressItem);
    if (choices == null) {
      return false;
    }
    final ArrayAdapter<String> choicesAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            choices.first);

    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Operation?");
    dialogBuilder.setAdapter(choicesAdapter, this);
    AlertDialog dialog = dialogBuilder.create();
    dialog.show();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  protected void setProgressIndicator(int change) {
    mRequestCount += change;
    setProgressBarIndeterminateVisibility(mRequestCount > 0);
  }

  protected void setRefreshResult() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("refresh", true);
    setResult(Activity.RESULT_OK, resultIntent);
  }
}
