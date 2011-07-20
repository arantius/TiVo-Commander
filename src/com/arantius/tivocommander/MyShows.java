package com.arantius.tivocommander;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class MyShows extends ListActivity {
  private JsonNode mItems;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Context context = this;

    final OnItemClickListener onClickListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        JsonNode item = mItems.get(position);
        JsonNode countNode = item.get("folderItemCount");
        if (countNode != null && countNode.getValueAsInt() > 0) {
          Intent intent = new Intent(getBaseContext(), MyShows.class);
          String folderId = item.get("recordingFolderItemId").getValueAsText();
          intent.putExtra("com.arantius.tivocommander.folderId", folderId);
          String folderName = item.get("title").getValueAsText();
          intent.putExtra("com.arantius.tivocommander.folderName", folderName);
          startActivity(intent);
        } else {
          MindRpc.addRequest(new UiNavigate(item), null);
        }
      }
    };

    final MindRpcResponseListener detailCallback;
    detailCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        mItems = response.getBody().get("recordingFolderItem");
        String[] titles = new String[mItems.size()];
        for (int i = 0; i < mItems.size(); i++) {
          titles[i] = mItems.get(i).get("title").getValueAsText();
        }

        setListAdapter(new ArrayAdapter<String>(context,
            android.R.layout.simple_list_item_1, titles));
        final ListView lv = getListView();
        lv.setOnItemClickListener(onClickListener);
      }
    };

    MindRpcResponseListener idSequenceCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        JsonNode ids = response.getBody().findValue("objectIdAndType");
        MindRpc.addRequest(new RecordingFolderItemSearch(ids), detailCallback);
      }
    };

    Bundle bundle = getIntent().getExtras();
    String folderId = null;
    if (bundle != null) {
      folderId = bundle.getString("com.arantius.tivocommander.folderId");
      setTitle("TiVo Commander - "
          + bundle.getString("com.arantius.tivocommander.folderName"));
    } else {
      setTitle("TiVo Commander - My Shows");
    }
    MindRpc.addRequest(new RecordingFolderItemSearch(folderId),
        idSequenceCallback);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
