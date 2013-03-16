/*
DVR Commander for TiVo allows control of a TiVo Premiere device.
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

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.TodoSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class ToDo extends ShowList {
  protected int getIconForItem(JsonNode item) {
    if ("inProgress".equals(item.path("state").asText())) {
      return R.drawable.recording_recording;
    }

    final String subscriptionType = item.path("subscriptionIdentifier")
        .path(0).path("subscriptionType").asText();
    if ("seasonPass".equals(subscriptionType)) {
      return R.drawable.todo_seasonpass;
    } else if ("wishList".equals(subscriptionType)) {
      return R.drawable.todo_wishlist;
    } else if ("singleOffer".equals(subscriptionType)) {
      return R.drawable.todo_recording;
    }

    return R.drawable.blank;
  }

  protected JsonNode getRecordingFromItem(JsonNode item) {
    return item.path("recordingForChildRecordingId");
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }

    if (EXPECT_REFRESH_INTENT_ID == requestCode) {
      if (data.getBooleanExtra("refresh", false)) {
        setRefreshResult();
        if (mShowData.size() == 1) {
          // We deleted the last show! Go up a level.
          finishWithRefresh();
        } else {
          // Load the list of remaining shows.
          startRequest();
        }
      }
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (MindRpc.init(this, null)) {
      return;
    }

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list_todo);
    setTitle("To Do List");

    mListAdapter = new ShowsAdapter(this);
    getListView().setAdapter(mListAdapter);
    getListView().setOnItemClickListener(mOnClickListener);

    mDetailCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);

            String itemId = "recording";
            final JsonNode items = response.getBody().path(itemId);

            ArrayList<Integer> slotMap = mRequestSlotMap.get(response.getRpcId());

            for (int i = 0; i < items.size(); i++) {
              int pos = slotMap.get(i);
              JsonNode item = items.get(i);
              mShowData.set(pos, item);
              mShowStatus.set(pos, ShowStatus.LOADED);
            }

            mRequestSlotMap.remove(response.getRpcId());
            mListAdapter.notifyDataSetChanged();
          }
        };

    mIdSequenceCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            JsonNode body = response.getBody();

            setProgressIndicator(-1);

            mShowIds = (ArrayNode) body.findValue("objectIdAndType");

            mShowData.clear();
            mShowStatus.clear();
            for (int i = 0; i < mShowIds.size(); i++) {
              mShowData.add(null);
              mShowStatus.add(ShowStatus.MISSING);
            }
            mListAdapter.notifyDataSetChanged();
          }
        };

    startRequest();
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:ToDo");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:ToDo");
    MindRpc.init(this, null);
  }

  protected void startRequest() {
    MindRpc.addRequest(new TodoSearch(), mIdSequenceCallback);
    setProgressIndicator(1);
  }
}
