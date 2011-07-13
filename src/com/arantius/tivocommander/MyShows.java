package com.arantius.tivocommander;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.response.IdSequenceResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.arantius.tivocommander.rpc.response.RecordingFolderItemListResponse;

public class MyShows extends ListActivity {
  private static final String LOG_TAG = "tivo_commander";
  private ArrayList<RecordingFolderItemListResponse.RecordingFolderItem> mItems =
      new ArrayList<RecordingFolderItemListResponse.RecordingFolderItem>();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    final Context context = this;

    final OnItemClickListener onClickListener = new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        Log.d(LOG_TAG, "clicked! " + mItems.get(position).getRecordingId());
      }
    };

    final MindRpcResponseListener detailCallback;
    detailCallback = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse responseGeneric) {
        RecordingFolderItemListResponse response =
            (RecordingFolderItemListResponse) responseGeneric;
        mItems = response.getItems();
        Log.i(LOG_TAG, "Got second ... " + Integer.toString(mItems.size()));
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

    MindRpc.addRequest(new RecordingFolderItemSearch(), idSequenceCallback);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
