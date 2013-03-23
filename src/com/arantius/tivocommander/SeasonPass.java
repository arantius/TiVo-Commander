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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.SubscriptionSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.mobeta.android.dslv.DragSortListView;

// TODO: This copies a lot from ShowList; be DRY?

public class SeasonPass extends ListActivity {
  protected class SubscriptionAdapter extends ArrayAdapter<JsonNode> {
    public SubscriptionAdapter() {
      super(SeasonPass.this, 0, mSubscriptionData);

      mBlankLogoDrawable = new ColorDrawable(0x00000000);
      Rect r = new Rect(0, 0, 65, 55); // Logo images are 65x55 px.
      mBlankLogoDrawable.setBounds(r);
    }

    protected ColorDrawable mBlankLogoDrawable;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (mSubscriptionStatus.get(position) == SubscriptionStatus.MISSING) {
        // If the data for this position is missing, fetch it and more, if they
        // exist, up to a limit of MAX_REQUEST_BATCH.
        ArrayList<JsonNode> subscriptionIds = new ArrayList<JsonNode>();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        int i = position;
        while (i < mSubscriptionData.size()) {
          if (mSubscriptionStatus.get(i) == SubscriptionStatus.MISSING) {
            JsonNode subscriptionId = mSubscriptionIds.get(i);
            subscriptionIds.add(subscriptionId);
            slots.add(i);
            mSubscriptionStatus.set(i, SubscriptionStatus.LOADING);
            if (subscriptionIds.size() >= MAX_REQUEST_BATCH) {
              break;
            }
          }
          i++;
        }

        SubscriptionSearch req = new SubscriptionSearch(subscriptionIds);
        mRequestSlotMap.put(req.getRpcId(), slots);
        MindRpc.addRequest(req, mDetailCallback);
      }

      LayoutInflater vi =
          (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      if (mSubscriptionStatus.get(position) == SubscriptionStatus.LOADED) {
        // If this item is available, display it.
        v = vi.inflate(R.layout.item_season_pass, null);
        v.setLayoutParams(new AbsListView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT));

        final JsonNode item = mSubscriptionData.get(position);

        ((TextView) v.findViewById(R.id.index)).setText(
            Integer.toString(position + 1));

        ((TextView) v.findViewById(R.id.show_title)).setText(
            Utils.stripQuotes(item.path("title").asText()));

        ((ImageView) v.findViewById(R.id.icon_until_deleted)).setVisibility(
            "forever".equals(item.path("keepBehavior").asText())
                ? View.VISIBLE : View.GONE);
        ((TextView) v.findViewById(R.id.keep_num)).setText(
            item.path("maxRecordings").asText());

        final boolean newOnly =
            "firstRunOnly".equals(item.path("showStatus").asText());
        final int newOnlyVis = newOnly ? View.VISIBLE : View.GONE;
        ((TextView) v.findViewById(R.id.new_comma)).setVisibility(newOnlyVis);
        ((ImageView) v.findViewById(R.id.badge_new)).setVisibility(newOnlyVis);
        ((TextView) v.findViewById(R.id.new_only)).setVisibility(newOnlyVis);

        final JsonNode channel = item.path("idSetSource").path("channel");
        TextView channelView = (TextView) v.findViewById(R.id.show_channel);
        channelView.setText(
            channel.path("channelNumber").asText());
        channelView.setCompoundDrawables(null, null, null, mBlankLogoDrawable);
        if (channel.has("logoIndex")) {
          // Wish lists don't have channels, so get the image conditionally.
          final String channelLogoUrl =
              "http://" + MindRpc.mTivoAddr + "/ChannelLogo/icon-" +
                  channel.path("logoIndex") + "-1.png";
          new DownloadImageTask(SeasonPass.this, channelView)
              .execute(channelLogoUrl);
        }
      } else {
        // Otherwise give a loading indicator.
        v = vi.inflate(R.layout.progress, null);
      }

      return v;
    }

  }

  protected final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          final JsonNode item = mSubscriptionData.get(position);
          if (item == null) {
            return;
          }

          final String collectionId =
              item.path("idSetSource").path("collectionId")
                  .asText();
          if ("".equals(collectionId)) {
            return;
          }

          Intent intent = new Intent(SeasonPass.this, ExploreTabs.class);
          intent.putExtra("collectionId", collectionId);
          startActivity(intent);
        }

      };

  protected enum SubscriptionStatus {
    LOADED, LOADING, MISSING;
  }

  protected final static int MAX_REQUEST_BATCH = 5;

  protected MindRpcResponseListener mDetailCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          final JsonNode items = response.getBody().path("subscription");

          ArrayList<Integer> slotMap =
              mRequestSlotMap.get(response.getRpcId());

          for (int i = 0; i < items.size(); i++) {
            int pos = slotMap.get(i);
            JsonNode item = items.get(i);
            mSubscriptionData.set(pos, item);
            mSubscriptionStatus.set(pos, SubscriptionStatus.LOADED);
          }

          mRequestSlotMap.remove(response.getRpcId());
          mListAdapter.notifyDataSetChanged();
        }
      };

  protected SubscriptionAdapter mListAdapter;
  protected boolean mReorderMode = false;
  protected final SparseArray<ArrayList<Integer>> mRequestSlotMap =
      new SparseArray<ArrayList<Integer>>();
  protected final ArrayList<SubscriptionStatus> mSubscriptionStatus =
      new ArrayList<SubscriptionStatus>();
  protected final ArrayList<JsonNode> mSubscriptionData =
      new ArrayList<JsonNode>();
  protected ArrayNode mSubscriptionIds;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (MindRpc.init(this, null)) {
      return;
    }

    Utils.activateHomeButton(this);
    setTitle("Season Pass Manager");

    setContentView(R.layout.list_season_pass);

    mListAdapter = new SubscriptionAdapter();
    setListAdapter(mListAdapter);
    DragSortListView dslv = (DragSortListView) getListView();
    dslv.setOnItemClickListener(mOnClickListener);

    MindRpcResponseListener idSequenceCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            JsonNode body = response.getBody();

            mSubscriptionIds = (ArrayNode) body.findValue("objectIdAndType");
            for (int i = 0; i < mSubscriptionIds.size(); i++) {
              mSubscriptionData.add(null);
              mSubscriptionStatus.add(SubscriptionStatus.MISSING);
            }
            mListAdapter.notifyDataSetChanged();
          }
        };
    MindRpc.addRequest(new SubscriptionSearch(), idSequenceCallback);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:About");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:About");
    if (MindRpc.init(this, null)) {
      return;
    }
  }

  public void reorderEnable(View unusedView) {
    mReorderMode = true;
    findViewById(R.id.reorder_enable).setVisibility(View.GONE);
    findViewById(R.id.reorder_apply).setVisibility(View.VISIBLE);
//    mListAdapter.notifyDataSetChanged();
  }

  public void reorderApply(View unusedView) {
    mReorderMode = false;
    findViewById(R.id.reorder_enable).setVisibility(View.VISIBLE);
    findViewById(R.id.reorder_apply).setVisibility(View.GONE);
//    mListAdapter.notifyDataSetChanged();
    // TODO: Dialog and RPC.
  }
}
