package com.arantius.tivocommander;

import android.app.ListActivity;
import android.os.Bundle;
import android.util.Log;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingFolderItemSearch;
import com.arantius.tivocommander.rpc.response.IdSequenceResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class MyShows extends ListActivity {
  private static final String LOG_TAG = "tivo_commander";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Request the list of shows ...
    RecordingFolderItemSearch request = new RecordingFolderItemSearch();
    MindRpcResponseListener listener = new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse responseGeneric) {
        IdSequenceResponse response = (IdSequenceResponse) responseGeneric;
        // ... then details for each.
        RecordingFolderItemSearch request =
            new RecordingFolderItemSearch(response.getIds());
        Log.i(LOG_TAG, "Got first ...");
        MindRpc.addRequest(request, new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            Log.i(LOG_TAG, "Got second ...");
          }
        });
      }
    };
    MindRpc.addRequest(request, listener);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
