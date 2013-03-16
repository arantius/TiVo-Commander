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

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.request.TodoRecordingSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public abstract class ShowList extends ListActivity {
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
          } else  if (mContext instanceof MyShows) {
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
        v = vi.inflate(R.layout.item_my_shows, null);
        final JsonNode item = mShowData.get(position);
        final JsonNode recording = getRecordingFromItem(item);

        String title = item.path("title").getTextValue();
        if ('"' == title.charAt(0) && '"' == title.charAt(title.length() - 1)) {
          title = title.substring(1, title.length() - 1);
        }
        ((TextView) v.findViewById(R.id.show_title)).setText(title);

        Integer folderItemCount = item.path("folderItemCount").getIntValue();
        ((TextView) v.findViewById(R.id.folder_num))
            .setText(folderItemCount > 0 ? folderItemCount.toString() : "");

        String channelStr = "";
        JsonNode channel = recording.path("channel");
        if (folderItemCount == 0 && !channel.isMissingNode()) {
          channelStr =
              String.format("%s %s", channel.path("channelNumber")
                  .getTextValue(), channel.path("callSign").getTextValue());
        }
        ((TextView) v.findViewById(R.id.show_channel)).setText(channelStr);

        String startTimeStr = item.path("startTime").getTextValue();
        if (startTimeStr == null) {
          // Rarely the time is only on the recording, not the item.
          startTimeStr = recording.path("startTime").getTextValue();
        }

        if (startTimeStr == null || "1970".equals(startTimeStr.substring(0, 4))) {
          v.findViewById(R.id.show_time).setVisibility(View.GONE);
        } else {
          Date startTime =
              Utils.parseDateTimeStr(startTimeStr);
          SimpleDateFormat dateFormatter =
              new SimpleDateFormat("EEE M/d", Locale.US);
          ((TextView) v.findViewById(R.id.show_time)).setText(dateFormatter
              .format(startTime));
        }

        final int iconId = getIconForItem(item);
        ((ImageView) v.findViewById(R.id.show_icon))
            .setImageDrawable(getResources().getDrawable(iconId));
      } else {
        // Otherwise give a loading indicator.
        v = vi.inflate(R.layout.progress, null);
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
                .getTextValue());
            intent.putExtra("collectionId", recording.path("collectionId")
                .getTextValue());

            // Regular / deleted recordings IDs are differently located.
            if (item.has("childRecordingId")) {
              intent.putExtra("recordingId", item.path("childRecordingId")
                  .getTextValue());
            } else if (item.has("recordingId")) {
              intent.putExtra("recordingId", item.path("recordingId")
                  .getTextValue());
            }

            startActivityForResult(intent, EXPECT_REFRESH_INTENT_ID);
          }
        }
      };

  protected abstract int getIconForItem(JsonNode item);

  protected void finishWithRefresh() {
    setRefreshResult();
    finish();
  }

  private JsonNode getRecordingFromItem(JsonNode item) {
    if ("deleted".equals(mFolderId)) {
      // In deleted mode, we directly fetch recordings.
      return item;
    } else {
      // Otherwise we've got recordings wrapped in folders.
      return item.path("recordingForChildRecordingId");
    }
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
        }
      }
    }
  };

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return Utils.onCreateOptionsMenu(menu, this);
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

  protected void startRequest() {
  }
}
