package com.arantius.tivocommander;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
            // TODO: Filter items in the past.
            HashMap<String, String> listItem = new HashMap<String, String>();

            String details =
                String.format(" %s  %s %s", formatTime(item),
                    item.path("channel").path("channelNumber").getTextValue(),
                    item.path("channel").path("callSign").getTextValue());
            if (item.path("episodic").getBooleanValue()) {
              // @formatter:off
              details = String.format("(Sea %d Ep %d)  ",
                  item.path("seasonNumber").getIntValue(),
                  item.path("episodeNum").path(0).getIntValue()) + details;
              // @formatter:on
            }
            listItem.put("details", details);
            listItem.put("title", item.has("subtitle") ? item.path("subtitle")
                .getTextValue() : item.path("title").getTextValue());
            listItems.add(listItem);
          }

          final ListView lv = getListView();
          lv.setAdapter(new SimpleAdapter(mContext, listItems,
              R.layout.item_upcoming, new String[] { "details", "title" },
              new int[] { R.id.upcoming_details, R.id.upcoming_title }));
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

  protected String formatTime(JsonNode item) {
    String timeIn = item.path("startTime").getTextValue();
    if (timeIn == null) {
      return null;
    }

    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    ParsePosition pp = new ParsePosition(0);
    Date playTime = dateParser.parse(timeIn, pp);
    SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE M/d hh:mm a");
    return dateFormatter.format(playTime);
  }
}
