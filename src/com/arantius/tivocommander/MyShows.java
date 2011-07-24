package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
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
import com.arantius.tivocommander.rpc.request.UiNavigate;
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

    public int getCount() {
      return mSize;
    }

    public Object getItem(int position) {
      return null;
    }

    public long getItemId(int position) {
      return 0;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
      return mInflater.inflate(R.layout.progress, parent, false);
    }
  }

  private static final int INTENT_CONTENT = 1;
  private final Context mContext = this;

  private final MindRpcResponseListener mDetailCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mItems = response.getBody().path("recordingFolderItem");
          List<HashMap<String, Object>> listItems =
              new ArrayList<HashMap<String, Object>>();

          for (int i = 0; i < mItems.size(); i++) {
            final JsonNode item = mItems.path(i);
            HashMap<String, Object> listItem = new HashMap<String, Object>();

            String title = item.path("title").getTextValue();
            if ('"' == title.charAt(0)
                && '"' == title.charAt(title.length() - 1)) {
              title = title.substring(1, title.length() - 1);
            }
            listItem.put("title", title);
            listItem.put("icon", getIconForItem(item));
            listItem.put("more", R.drawable.more);
            if (item.path("folderItemCount").getIntValue() == 0
                && getContentIdForItem(item) == null) {
              listItem.put("more", R.drawable.blank);
            }
            listItems.add(listItem);
          }

          final ListView lv = getListView();
          lv.setAdapter(new SimpleAdapter(mContext, listItems,
              R.layout.list_my_shows, new String[] { "icon", "more", "title" },
              new int[] { R.id.show_icon, R.id.show_more, R.id.show_title }));
          lv.setOnItemClickListener(mOnClickListener);
        }
      };

  private String mFolderId;

  private final MindRpcResponseListener mIdSequenceCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode ids = response.getBody().findValue("objectIdAndType");
          MindRpc.addRequest(new RecordingFolderItemSearch(ids),
              mDetailCallback);

          // Show the right number of progress throbbers while loading details.
          setListAdapter(new ProgressAdapter(mContext, ids.size()));
        }
      };
  private JsonNode mItems;

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
            String contentId = getContentIdForItem(item);
            if (contentId == null) {
              String recordingId = item.path("childRecordingId").getTextValue();
              MindRpc.addRequest(new UiNavigate(recordingId), null);
            } else {
              // Navigate to 'content' for this item.
              Intent intent = new Intent(MyShows.this, Content.class);
              intent.putExtra("contentId", contentId);
              startActivityForResult(intent, INTENT_CONTENT);
            }
          }
        }
      };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

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
    // Replace any old data that might exist with a progress throbber.
    setListAdapter(new ProgressAdapter(mContext, 0));
    // Get new data.
    MindRpc.addRequest(new RecordingFolderItemSearch(mFolderId),
        mIdSequenceCallback);
  }

  protected final String getContentIdForItem(JsonNode item) {
    return item.path("recordingForChildRecordingId").path("contentId")
        .getTextValue();
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

    if (requestCode == INTENT_CONTENT) {
      if (data.getBooleanExtra("refresh", false)) {
        startRequest();
      }
    }
  }
}
