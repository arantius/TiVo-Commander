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

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Window;
import android.widget.ListView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.TodoSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ToDo extends ShowList {
  protected int getIconForItem(JsonNode item) {
    if (item == null) return R.drawable.blank;
    SubscriptionType subType = Utils.subscriptionTypeForRecording(item);
    if (subType == null) return R.drawable.blank;

    switch (subType) {
    case RECORDING:
      return R.drawable.recording_recording;
    case SEASON_PASS:
      return R.drawable.todo_seasonpass;
    case SINGLE_OFFER:
      return R.drawable.todo_single_offer;
    case WISHLIST:
      return R.drawable.todo_wishlist;
    }

    return R.drawable.blank;
  }

  protected Pair<ArrayList<String>, ArrayList<Integer>> getLongPressChoices(
      JsonNode item) {
    final ArrayList<String> choices = new ArrayList<String>();
    final ArrayList<Integer> actions = new ArrayList<Integer>();

    if ("inProgress".equals(item.path("state").asText())) {
      choices.add(getResources().getString(R.string.stop_recording));
      actions.add(R.string.stop_recording);
      choices.add(getResources().getString(R.string.stop_recording_and_delete));
      actions.add(R.string.stop_recording_and_delete);
    } else {
      choices.add(getResources().getString(R.string.dont_record));
      actions.add(R.string.dont_record);
    }

    return Pair.create(choices, actions);
  }

  protected JsonNode getRecordingFromItem(JsonNode item) {
    return item;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Assume we've been asked to refresh, restart the activity.
    startActivity(getIntent());
    finish();
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
    ListView lv = getListView();
    lv.setAdapter(mListAdapter);
    lv.setOnItemClickListener(mOnClickListener);
    lv.setLongClickable(true);
    lv.setOnItemLongClickListener(this);

    mDetailCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            setProgressIndicator(-1);

            String itemId = "recording";
            final JsonNode items = response.getBody().path(itemId);

            ArrayList<Integer> slotMap =
                mRequestSlotMap.get(response.getRpcId());

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
            if (mShowIds == null) return;

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
    mShowData.clear();
    mShowStatus.clear();
    mListAdapter.notifyDataSetChanged();
    MindRpc.addRequest(new TodoSearch(), mIdSequenceCallback);
    setProgressIndicator(1);
  }
}
