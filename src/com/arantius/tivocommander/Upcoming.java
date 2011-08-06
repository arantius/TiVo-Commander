package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.UpcomingSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Upcoming extends ListActivity {
  private final MindRpcResponseListener mUpcomingListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mShows = response.getBody().path("offer");
          List<HashMap<String, String>> listItems =
              new ArrayList<HashMap<String, String>>();

          for (int i = 0; i < mShows.size(); i++) {
            final JsonNode item = mShows.path(i);
            HashMap<String, String> listItem = new HashMap<String, String>();

            // @formatter:off
            listItem.put("details", String.format("(Sea %d Ep %d) %s %s %s",
                item.path("seasonNumber").getIntValue(),
                item.path("episodeNum").path(0).getIntValue(),
                item.path("startTime").getTextValue(),
                item.path("channel").path("channelNumber").getTextValue(),
                item.path("channel").path("callSign").getTextValue()
                ));
            // @formatter:on
            listItem.put("subtitle", item.path("subtitle").getTextValue());
            listItems.add(listItem);
          }

          final ListView lv = getListView();
          lv.setAdapter(new SimpleAdapter(mContext, listItems,
              R.layout.item_upcoming, new String[] { "details", "subtitle" },
              new int[] { R.id.upcoming_details, R.id.upcoming_subtitle }));
//          lv.setOnItemClickListener(mOnClickListener);
        }
      };
  private Activity mContext;
  protected JsonNode mShows;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);
    mContext = this;

    Bundle bundle = getIntent().getExtras();
    String collectionId;

    if (bundle != null) {
      collectionId = bundle.getString("collectionId");
      if (collectionId == null) {
        Toast.makeText(getApplicationContext(), "Oops; missing collection ID",
            Toast.LENGTH_SHORT).show();
      } else {
        UpcomingSearch request = new UpcomingSearch(collectionId);
        MindRpc.addRequest(request, mUpcomingListener);
      }
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
