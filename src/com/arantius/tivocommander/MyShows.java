package com.arantius.tivocommander;

import java.util.ArrayList;

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
import com.arantius.tivocommander.rpc.response.IdSequenceResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.arantius.tivocommander.rpc.response.RecordingFolderItemListResponse;

public class MyShows extends ListActivity {
  private final ArrayList<RecordingFolderItemListResponse.RecordingFolderItem> mItems =
      new ArrayList<RecordingFolderItemListResponse.RecordingFolderItem>();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onResume() {
    super.onResume();

    final Context context = this;

    final OnItemClickListener onClickListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        RecordingFolderItemListResponse.RecordingFolderItem item;
        item = mItems.get(position);
        if (item.getFolderItemCount() > 0) {
          Intent intent = new Intent(getBaseContext(), MyShows.class);
          intent.putExtra("com.arantius.tivocommander.folderId",
              item.getRecordingFolderId());
          startActivity(intent);
        } else {
          MindRpc.addRequest(new UiNavigate(item), null);
        }
      }
    };

    final MindRpcResponseListener detailCallback;
    detailCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse responseGeneric) {
        RecordingFolderItemListResponse response =
            (RecordingFolderItemListResponse) responseGeneric;
        mItems.clear();
        mItems.addAll(response.getItems());
        String[] titles = new String[mItems.size()];
        for (int i = 0; i < mItems.size(); i++) {
          titles[i] = mItems.get(i).getTitle();
        }

        setListAdapter(new ArrayAdapter<String>(context,
            android.R.layout.simple_list_item_1, titles));
        final ListView lv = getListView();
        lv.setOnItemClickListener(onClickListener);
      }
    };

    MindRpcResponseListener idSequenceCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse responseGeneric) {
        IdSequenceResponse response = (IdSequenceResponse) responseGeneric;
        MindRpc.addRequest(new RecordingFolderItemSearch(response.getIds()),
            detailCallback);
      }
    };

    Bundle bundle = getIntent().getExtras();
    String folderId = null;
    if (bundle != null) {
      folderId = bundle.getString("com.arantius.tivocommander.folderId");
    }
    MindRpc.addRequest(new RecordingFolderItemSearch(folderId),
        idSequenceCallback);
    MindRpc.init(this);
  }
}
