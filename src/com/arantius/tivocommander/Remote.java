package com.arantius.tivocommander;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.arantius.tivocommander.rpc.request.KeyEventSend;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;

public class Remote extends ListActivity {
  static final String[] labels = { "Play", "Pause", "Rewind", "Fast forward",
      "Up", "Down", "Left", "Right", "Select" };
  static final String[] events = { "play", "pause", "reverse", "forward", "up",
      "down", "left", "right", "select" };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // setContentView(R.layout.main);
    setListAdapter(new ArrayAdapter<String>(this,
        android.R.layout.simple_list_item_1, labels));

    final ListView lv = getListView();
    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        MindRpcRequest request = new KeyEventSend(events[position]);
        Main.mRpc.addRequest(request);
      }
    });
  }
}
