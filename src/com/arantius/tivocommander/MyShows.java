package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class MyShows extends ListActivity {
  private class ProgressAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final int mSize;

    public ProgressAdapter(Context context, int size) {
      mInflater =
          (LayoutInflater) context
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      mSize = size;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      return mInflater.inflate(R.layout.progress, parent, false);
    }

    public int getCount() {
      return mSize;
    }

    public Object getItem(int position) {
      return null;
    }

    public long getItemId(int position) {
      return 0;
    }
  }

  private String mFolderId;
  private JsonNode mItems;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    final Context context = this;

    final OnItemClickListener onClickListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        JsonNode item = mItems.path(position);
        JsonNode countNode = item.path("folderItemCount");
        if (countNode != null && countNode.getValueAsInt() > 0) {
          // Navigate to 'my shows' for this folder.
          Intent intent = new Intent(getBaseContext(), MyShows.class);
          String folderId = item.path("recordingFolderItemId").getValueAsText();
          intent.putExtra("com.arantius.tivocommander.folderId", folderId);
          String folderName = item.path("title").getValueAsText();
          intent.putExtra("com.arantius.tivocommander.folderName", folderName);
          startActivity(intent);
        } else {
          // Navigate to 'content' for this item.
          Intent intent = new Intent(getBaseContext(), Content.class);
          String contentId =
              item.path("recordingForChildRecordingId").path("contentId")
                  .getTextValue();
          intent.putExtra("com.arantius.tivocommander.contentId", contentId);
          startActivity(intent);
        }
      }
    };

    final MindRpcResponseListener detailCallback;
    detailCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        mItems = response.getBody().path("recordingFolderItem");
        List<HashMap<String, Object>> listItems =
            new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < mItems.size(); i++) {
          final JsonNode item = mItems.path(i);
          HashMap<String, Object> listItem = new HashMap<String, Object>();

          String title = item.path("title").getTextValue();
          if ('"' == title.charAt(0) && '"' == title.charAt(title.length() - 1)) {
            title = title.substring(1, title.length() - 1);
          }
          listItem.put("title", title);

          listItem.put("icon", R.drawable.blank); // By default blank.
          if (item.has("folderTransportType")) {
            String folderTransportType =
                item.path("folderTransportType").path(0).getTextValue();
            if (folderTransportType.equals("mrv")) {
              listItem.put("icon", R.drawable.folder_downloading);
            } else if (folderTransportType.equals("stream")) {
              listItem.put("icon", R.drawable.folder_recording);
            }
          } else if (item.has("folderType")) {
            if (item.path("folderType").getTextValue().equals("wishlist")) {
              listItem.put("icon", R.drawable.folder_wishlist);
            } else {
              listItem.put("icon", R.drawable.folder);
            }
          } else if (item.has("folderItemCount")) {
            listItem.put("icon", R.drawable.folder);
          } else if (item.has("recordingStatusType")) {
            String recordingStatus =
                item.path("recordingStatusType").getTextValue();
            if (recordingStatus.equals("expired")) {
              listItem.put("icon", R.drawable.recording_expired);
            } else if (recordingStatus.equals("expiresSoon")) {
              listItem.put("icon", R.drawable.recording_expiressoon);
            } else if (recordingStatus.equals("inProgressDownload")) {
              listItem.put("icon", R.drawable.recording_downloading);
            } else if (recordingStatus.equals("inProgressRecording")) {
              listItem.put("icon", R.drawable.recording_recording);
            } else if (recordingStatus.equals("keepForever")) {
              listItem.put("icon", R.drawable.recording_keep);
            } else if (recordingStatus.equals("suggestion")) {
              listItem.put("icon", R.drawable.recording_suggestion);
            } else if (recordingStatus.equals("wishlist")) {
              listItem.put("icon", R.drawable.recording_wishlist);
            }
          } else if (item.has("recordingForChildRecordingId")) {
            JsonNode recording = item.path("recordingForChildRecordingId");
            if (recording.has("type")) {
              if (recording.path("type").getTextValue().equals("recording")) {
                listItem.put("icon", R.drawable.recording);
              }
            }
          }

          listItems.add(listItem);
        }

        final ListView lv = getListView();
        lv.setAdapter(new SimpleAdapter(context, listItems,
            R.layout.list_my_shows, new String[] { "icon", "title" },
            new int[] { R.id.show_icon, R.id.show_title }));
        lv.setOnItemClickListener(onClickListener);
      }
    };

    MindRpcResponseListener idSequenceCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        JsonNode ids = response.getBody().findValue("objectIdAndType");
        MindRpc.addRequest(new RecordingFolderItemSearch(ids), detailCallback);

        // Show the right number of progress throbbers while loading details.
        setListAdapter(new ProgressAdapter(context, ids.size()));
      }
    };

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mFolderId = bundle.getString("com.arantius.tivocommander.folderId");
      setTitle("TiVo Commander - "
          + bundle.getString("com.arantius.tivocommander.folderName"));
    } else {
      mFolderId = null;
      setTitle("TiVo Commander - My Shows");
    }
    MindRpc.addRequest(new RecordingFolderItemSearch(mFolderId),
        idSequenceCallback);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
