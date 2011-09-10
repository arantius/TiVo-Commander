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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.BodyConfigSearch;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class MyShows extends ListActivity {
  private class ShowsAdapter extends ArrayAdapter<JsonNode> {
    public ShowsAdapter(Context context) {
      super(context, 0, mShowData);
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
            showIds.add(mShowIds.get(i));
            slots.add(i);
            mShowStatus.set(i, ShowStatus.LOADING);
            if (showIds.size() >= MAX_SHOW_REQUEST_BATCH) {
              break;
            }
          }
          i++;
        }

        RecordingFolderItemSearch req =
            new RecordingFolderItemSearch(showIds, mOrderBy);
        mRequestSlotMap.put(req.getRpcId(), slots);
        MindRpc.addRequest(req, mDetailCallback);
        setProgressIndicator(1);
      }

      LayoutInflater vi =
          (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      if (mShowStatus.get(position) == ShowStatus.LOADED) {
        // If this item is available, display it.
        v = vi.inflate(R.layout.item_my_shows, null);
        final JsonNode item = mShowData.get(position);

        String title = item.path("title").getTextValue();
        if ('"' == title.charAt(0) && '"' == title.charAt(title.length() - 1)) {
          title = title.substring(1, title.length() - 1);
        }
        ((TextView) v.findViewById(R.id.show_title)).setText(title);

        Integer folderItemCount = item.path("folderItemCount").getIntValue();
        ((TextView) v.findViewById(R.id.folder_num))
            .setText(folderItemCount > 0 ? folderItemCount.toString() : "");

        if ("1970"
            .equals(item.path("startTime").getTextValue().substring(0, 4))) {
          v.findViewById(R.id.show_time).setVisibility(View.GONE);
        } else {
          Date startTime =
              Utils.parseDateTimeStr(item.path("startTime").getTextValue());
          SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE M/d");
          ((TextView) v.findViewById(R.id.show_time)).setText(dateFormatter
              .format(startTime));
        }

        int iconId = MyShows.getIconForItem(item);
        ((ImageView) v.findViewById(R.id.show_icon))
            .setImageDrawable(getResources().getDrawable(iconId));
      } else {
        // Otherwise give a loading indicator.
        v = vi.inflate(R.layout.progress, null);
      }

      return v;
    }
  }

  private enum ShowStatus {
    LOADED, LOADING, MISSING;
  }

  private final static int EXPLORE_INTENT_ID = 1;
  private final static int MAX_SHOW_REQUEST_BATCH = 5;

  protected final static int getIconForItem(JsonNode item) {
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

  private final MindRpcResponseListener mBodyConfigCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setProgressIndicator(-1);

          ProgressBar mMeter = (ProgressBar) findViewById(R.id.meter);
          TextView mMeterText = (TextView) findViewById(R.id.meter_text);
          JsonNode bodyConfig = response.getBody().path("bodyConfig").path(0);
          int used = bodyConfig.path("userDiskUsed").getIntValue();
          int size = bodyConfig.path("userDiskSize").getIntValue();

          mMeter.setMax(size);
          mMeter.setProgress(used);

          mMeterText.setText(String.format("%d%% Disk Used",
              (int) ((double) 100 * used / size)));
        }
      };
  private final MindRpcResponseListener mDetailCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setProgressIndicator(-1);

          JsonNode items = response.getBody().path("recordingFolderItem");
          ArrayList<Integer> slotMap = mRequestSlotMap.get(response.getRpcId());

          MindRpc.saveBodyId(items.path(0).path("bodyId").getTextValue());

          for (int i = 0; i < items.size(); i++) {
            int pos = slotMap.get(i);
            mShowData.set(pos, items.get(i));
            mShowStatus.set(pos, ShowStatus.LOADED);
          }

          mRequestSlotMap.remove(response.getRpcId());
          mListAdapter.notifyDataSetChanged();
        }
      };
  private String mFolderId;
  private final MindRpcResponseListener mIdSequenceCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode body = response.getBody();
          if ("error".equals(body.path("status").getTextValue())) {
            Utils.log("Handling mIdSequenceCallback error response by "
                + "finishWithRefresh()");
            finishWithRefresh();
            return;
          }

          setProgressIndicator(-1);
          mShowIds = body.findValue("objectIdAndType");

          // Start from nothing ...
          mShowData.clear();
          mShowStatus.clear();
          if (mShowIds != null) {
            // e.g. "Suggestions" can be present, but empty!
            for (int i = 0; i < mShowIds.size(); i++) {
              mShowData.add(null);
              mShowStatus.add(ShowStatus.MISSING);
            }
          }

          // And get them displayed.
          mListAdapter.notifyDataSetChanged();
        }
      };
  private ShowsAdapter mListAdapter;
  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode item = mShowData.get(position);
          if (item == null) {
            return;
          }

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
  private String mOrderBy = "startTime";
  private final String[] mOrderLabels = new String[] { "Date", "A-Z" };
  private final String[] mOrderValues = new String[] { "startTime", "title" };
  private int mRequestCount = 0;
  private final HashMap<Integer, ArrayList<Integer>> mRequestSlotMap =
      new HashMap<Integer, ArrayList<Integer>>();
  private final ArrayList<JsonNode> mShowData = new ArrayList<JsonNode>();
  private JsonNode mShowIds;
  private final ArrayList<ShowStatus> mShowStatus = new ArrayList<ShowStatus>();

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

  private void finishWithRefresh() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("refresh", true);
    setResult(Activity.RESULT_OK, resultIntent);
    finish();
  }

  private void startRequest() {
    MindRpc.addRequest(new RecordingFolderItemSearch(mFolderId, mOrderBy),
        mIdSequenceCallback);
    setProgressIndicator(1);
    MindRpc.addRequest(new BodyConfigSearch(), mBodyConfigCallback);
    setProgressIndicator(1);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }

    if (EXPLORE_INTENT_ID == requestCode) {
      if (data.getBooleanExtra("refresh", false)) {
        if (mShowData.size() == 1) {
          // We deleted the last show! Go up a level.
          finishWithRefresh();
        } else {
          // Load the list of remaining shows.
          startRequest();
        }
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mFolderId = bundle.getString("folderId");
      setTitle("TiVo Commander - " + bundle.getString("folderName"));
    } else {
      mFolderId = null;
    }

    Utils.log(String.format("MyShows: folderId:%s", mFolderId));

    MindRpc.init(this);

    // TODO: Sorting.
    // TODO: Show disk usage.
    // TODO: Show date recorded.

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list_my_shows);

    if (mFolderId != null) {
      findViewById(R.id.button1).setVisibility(View.GONE);
    }

    mListAdapter = new ShowsAdapter(this);
    getListView().setAdapter(mListAdapter);
    getListView().setOnItemClickListener(mOnClickListener);

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
    MindRpc.init(this);
  }

  protected void setProgressIndicator(int change) {
    mRequestCount += change;
    setProgressBarIndeterminateVisibility(mRequestCount > 0);
  }
}
