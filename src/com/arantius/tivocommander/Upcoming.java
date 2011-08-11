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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.UpcomingSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Upcoming extends ListActivity {
  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode show = mShows.path(position);

          Intent intent = new Intent(Upcoming.this, ExploreTabs.class);
          intent.putExtra("contentId", show.path("contentId").getTextValue());
          intent.putExtra("collectionId", show.path("collectionId")
              .getTextValue());
          startActivity(intent);
        }
      };
  private final MindRpcResponseListener mUpcomingListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mShows = response.getBody().path("offer");

          // TODO: No results.

          List<HashMap<String, Object>> listItems =
              new ArrayList<HashMap<String, Object>>();

          for (int i = 0; i < mShows.size(); i++) {
            final JsonNode item = mShows.path(i);
            // TODO: Filter items in the past.
            HashMap<String, Object> listItem = new HashMap<String, Object>();

            String details =
                String.format(" %s  %s %s", formatTime(item),
                    item.path("channel").path("channelNumber").getTextValue(),
                    item.path("channel").path("callSign").getTextValue());
            if (item.path("episodic").getBooleanValue()) {
              // TODO: Not if season & episode 0 (e.g. Nova).
              // @formatter:off
              details = String.format("(Sea %d Ep %d)  ",
                  item.path("seasonNumber").getIntValue(),
                  item.path("episodeNum").path(0).getIntValue()) + details;
              // @formatter:on
            }
            listItem.put("icon", R.drawable.blank);
            if (item.has("recordingForOfferId")) {
              listItem.put("icon", R.drawable.check);
            }
            listItem.put("details", details);
            listItem.put("title", item.has("subtitle") ? item.path("subtitle")
                .getTextValue() : item.path("title").getTextValue());
            listItems.add(listItem);
          }

          final ListView lv = getListView();
          lv.setAdapter(new SimpleAdapter(mContext, listItems,
              R.layout.item_upcoming,
              new String[] { "details", "icon", "title" }, new int[] {
                  R.id.upcoming_details, R.id.upcoming_icon,
                  R.id.upcoming_title }));
          lv.setOnItemClickListener(mOnClickListener);
        }
      };
  private Activity mContext;

  protected JsonNode mShows;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);
    mContext = this;

    // TODO: Progress throbber.

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
