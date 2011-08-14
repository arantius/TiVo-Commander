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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
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
  private class DateInPast extends Throwable {
    private static final long serialVersionUID = -4184008452910054505L;
  }

  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode show = mShows.path(position);

          Intent intent = new Intent(Upcoming.this, ExploreTabs.class);
          intent.putExtra("contentId", show.path("contentId").getTextValue());
          intent.putExtra("collectionId", show.path("collectionId")
              .getTextValue());
          intent.putExtra("offerId", show.path("offerId").getTextValue());
          startActivity(intent);
        }
      };
  private final MindRpcResponseListener mUpcomingListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setProgressBarIndeterminateVisibility(false);

          mShows = response.getBody().path("offer");

          // TODO: No results.

          List<HashMap<String, Object>> listItems =
              new ArrayList<HashMap<String, Object>>();

          for (int i = 0; i < mShows.size(); i++) {
            final JsonNode item = mShows.path(i);
            try {
              HashMap<String, Object> listItem = new HashMap<String, Object>();

              String channelNum =
                  item.path("channel").path("channelNumber").getTextValue();
              String callSign =
                  item.path("channel").path("callSign").getTextValue();
              String details =
                  String.format("%s  %s %s", formatTime(item), channelNum,
                      callSign);
              if (item.path("episodic").getBooleanValue()
                  && item.path("episodeNumber").path(0).getIntValue() > 0
                  && item.path("seasonNumber").getIntValue() > 0) {
                details =
                    String.format("(Sea %d Ep %d)  ", item.path("seasonNumber")
                        .getIntValue(), item.path("episodeNum").path(0)
                        .getIntValue())
                        + details;
              }
              listItem.put("icon", R.drawable.blank);
              if (item.has("recordingForOfferId")) {
                listItem.put("icon", R.drawable.check);
              }
              listItem.put("details", details);
              listItem.put("title", item.has("subtitle") ? item
                  .path("subtitle").getTextValue() : item.path("title")
                  .getTextValue());
              listItems.add(listItem);
            } catch (DateInPast e) {
              // No-op. Just don't show past items.
            }
          }

          final ListView lv = getListView();
          lv.setAdapter(new SimpleAdapter(Upcoming.this, listItems,
              R.layout.item_upcoming,
              new String[] { "details", "icon", "title" }, new int[] {
                  R.id.upcoming_details, R.id.upcoming_icon,
                  R.id.upcoming_title }));
          lv.setOnItemClickListener(mOnClickListener);
        }
      };

  protected JsonNode mShows;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list);

    Bundle bundle = getIntent().getExtras();
    String collectionId;

    if (bundle != null) {
      collectionId = bundle.getString("collectionId");
      if (collectionId == null) {
        Toast.makeText(getApplicationContext(), "Oops; missing collection ID",
            Toast.LENGTH_SHORT).show();
      } else {
        setProgressBarIndeterminateVisibility(true);
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

  protected String formatTime(JsonNode item) throws DateInPast {
    String timeIn = item.path("startTime").getTextValue();
    if (timeIn == null) {
      return null;
    }

    Date playTime = Utils.parseDateStr(timeIn);
    if (playTime.before(new Date())) {
      throw new DateInPast();
    }
    SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE M/d hh:mm a");
    dateFormatter.setTimeZone(TimeZone.getDefault());
    return dateFormatter.format(playTime);
  }
}
