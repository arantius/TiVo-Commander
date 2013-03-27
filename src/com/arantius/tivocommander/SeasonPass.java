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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.SubscriptionSearch;
import com.arantius.tivocommander.rpc.request.SubscriptionsReprioritize;
import com.arantius.tivocommander.rpc.request.Unsubscribe;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.mobeta.android.dslv.DragSortListView;

// TODO: This copies a lot from ShowList; be DRY?

public class SeasonPass extends ListActivity implements
    OnItemLongClickListener, OnClickListener {
  protected class SubscriptionAdapter extends ArrayAdapter<JsonNode> {
    protected ColorDrawable mBlankLogoDrawable;

    public SubscriptionAdapter() {
      super(SeasonPass.this, 0, mSubscriptionData);

      mBlankLogoDrawable = new ColorDrawable(0x00000000);
      Rect r = new Rect(0, 0, 65, 55); // Logo images are 65x55 px.
      mBlankLogoDrawable.setBounds(r);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (mSubscriptionStatus.get(position) == SubscriptionStatus.MISSING) {
        // If the data for this position is missing, fetch it and more, if they
        // exist, up to a limit of MAX_REQUEST_BATCH.
        ArrayList<String> subscriptionIds = new ArrayList<String>();
        ArrayList<Integer> slots = new ArrayList<Integer>();
        int i = position;
        while (i < mSubscriptionData.size()) {
          if (mSubscriptionStatus.get(i) == SubscriptionStatus.MISSING) {
            String subscriptionId = mSubscriptionIds.get(i);
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
        String keepNum =item.path("maxRecordings").asText();
        if ("0".equals(keepNum)) {
          keepNum = "all";
        }
        ((TextView) v.findViewById(R.id.keep_num)).setText(keepNum);

        final boolean newOnly =
            "firstRunOnly".equals(item.path("showStatus").asText());
        final int newOnlyVis = newOnly ? View.VISIBLE : View.GONE;
        ((TextView) v.findViewById(R.id.new_comma)).setVisibility(newOnlyVis);
        ((ImageView) v.findViewById(R.id.badge_new)).setVisibility(newOnlyVis);
        ((TextView) v.findViewById(R.id.new_only)).setVisibility(newOnlyVis);

        TextView channelView = (TextView) v.findViewById(R.id.show_channel);
        ImageView dragHandle = (ImageView) v.findViewById(R.id.drag_handle);
        if (mInReorderMode) {
          dragHandle.setVisibility(View.VISIBLE);
          channelView.setVisibility(View.GONE);
        } else {
          dragHandle.setVisibility(View.GONE);
          channelView.setVisibility(View.VISIBLE);

          final JsonNode channel = item.path("idSetSource").path("channel");
          channelView.setText(
              channel.path("channelNumber").asText());
          channelView
              .setCompoundDrawables(null, null, null, mBlankLogoDrawable);
          if (channel.has("logoIndex")) {
            // Wish lists don't have channels, so get the image conditionally.
            final String channelLogoUrl =
                "http://" + MindRpc.mTivoAddr + "/ChannelLogo/icon-" +
                    channel.path("logoIndex") + "-1.png";
            new DownloadImageTask(SeasonPass.this, channelView)
                .execute(channelLogoUrl);
          }
        }
      } else {
        // Otherwise give a loading indicator.
        v = vi.inflate(R.layout.progress, null);
      }

      return v;
    }

  }

  protected enum SubscriptionStatus {
    LOADED, LOADING, MISSING;
  }

  protected final static int MAX_REQUEST_BATCH = 5;
  protected boolean mInReorderMode = false;
  protected SubscriptionAdapter mListAdapter;
  protected int mLongClickPosition;
  protected final SparseArray<ArrayList<Integer>> mRequestSlotMap =
      new SparseArray<ArrayList<Integer>>();
  protected final ArrayList<JsonNode> mSubscriptionData =
      new ArrayList<JsonNode>();
  protected ArrayList<String> mSubscriptionIds;
  protected ArrayList<String> mSubscriptionIdsBeforeReorder =
      new ArrayList<String>();
  protected final ArrayList<SubscriptionStatus> mSubscriptionStatus =
      new ArrayList<SubscriptionStatus>();

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

  final protected DragSortListView.DropListener mOnDrop =
      new DragSortListView.DropListener() {
        public void drop(int from, int to) {
          JsonNode item = mListAdapter.getItem(from);
          mListAdapter.remove(item);
          mListAdapter.insert(item, to);
          String id = mSubscriptionIds.get(from);
          mSubscriptionIds.remove(id);
          mSubscriptionIds.add(to, id);
        }
      };

  public void onClick(DialogInterface dialog, int which) {
    JsonNode sub = mSubscriptionData.get(mLongClickPosition);

    // TODO: De-dupe vs. Explore.doRecord().
    switch (which) {
    case 0:
      Intent intent =
          new Intent(getBaseContext(), SubscribeCollection.class);
      intent.putExtra("collectionId",
          sub.path("idSetSource").path("collectionId").asText());
      intent.putExtra("subscriptionId", sub.path("subscriptionId").asText());
      intent.putExtra("subscriptionJson", Utils.stringifyToJson(sub));
      startActivity(intent);
      break;
    case 1:
      setProgressBarIndeterminateVisibility(true);
      MindRpc.addRequest(new Unsubscribe(sub.path("subscriptionId").asText()),
          new MindRpcResponseListener() {
            public void onResponse(MindRpcResponse response) {
              setProgressBarIndeterminateVisibility(false);
              mSubscriptionData.remove(mLongClickPosition);
              mSubscriptionIds.remove(mLongClickPosition);
              mSubscriptionStatus.remove(mLongClickPosition);
              mListAdapter.notifyDataSetChanged();
            }
          });
      break;
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (MindRpc.init(this, null)) {
      return;
    }

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    Utils.activateHomeButton(this);
    setTitle("Season Pass Manager");
    setContentView(R.layout.list_season_pass);

    mListAdapter = new SubscriptionAdapter();
    setListAdapter(mListAdapter);
    DragSortListView dslv = (DragSortListView) getListView();
    dslv.setOnItemClickListener(mOnClickListener);
    dslv.setDropListener(mOnDrop);
    dslv.setLongClickable(true);
    dslv.setOnItemLongClickListener(this);

    MindRpcResponseListener idSequenceCallback =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            JsonNode body = response.getBody();

            mSubscriptionIds = new ArrayList<String>();
            for (JsonNode node : body.path("objectIdAndType")) {
              mSubscriptionIds.add(node.asText());
            }
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
  public boolean onCreateOptionsMenu(Menu menu) {
    return Utils.onCreateOptionsMenu(menu, this);
  }

  public boolean onItemLongClick(AdapterView<?> parent, View view,
      int position, long id) {
    mLongClickPosition = position;

    final ArrayList<String> choices = new ArrayList<String>();
    choices.add(Explore.RecordActions.SP_MODIFY.toString());
    choices.add(Explore.RecordActions.SP_CANCEL.toString());
    final ArrayAdapter<String> choicesAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            choices);

    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Operation?");
    dialogBuilder.setAdapter(choicesAdapter, this);
    AlertDialog dialog = dialogBuilder.create();
    dialog.show();

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:SeasonPass");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:SeasonPass");
    if (MindRpc.init(this, null)) {
      return;
    }
  }

  public void reorderApply(View unusedView) {
    Utils.log("SeasonPass::reorderApply() " + Boolean.toString(mInReorderMode));

    boolean noChange = true;
    ArrayList<String> subIds = new ArrayList<String>();
    for (int i = 0; i < mSubscriptionIds.size(); i++) {
      if (mSubscriptionIds.get(i) != mSubscriptionIdsBeforeReorder.get(i)) {
        noChange = false;
      }
      subIds.add(mSubscriptionData.get(i).path("subscriptionId").asText());
    }

    final ProgressDialog d = new ProgressDialog(this);
    final MindRpcResponseListener onReorderComplete =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            if (d.isShowing()) {
              d.dismiss();
            }

            // Flip the buttons.
            findViewById(R.id.reorder_enable).setVisibility(View.VISIBLE);
            findViewById(R.id.reorder_apply).setVisibility(View.GONE);
            // Turn off the drag handles.
            mInReorderMode = false;
            mListAdapter.notifyDataSetChanged();
          }
        };

    if (noChange) {
      // If there was no change, switch the UI back immediately.
      onReorderComplete.onResponse(null);
    } else {
      // Otherwise show a dialog while we do the RPC.
      d.setIndeterminate(true);
      d.setTitle("Saving ...");
      d.setMessage("Saving new season pass order.  "
          + "Patience please, this takes a while.");
      d.setCancelable(false);
      d.show();

      SubscriptionsReprioritize req = new SubscriptionsReprioritize(subIds);
      MindRpc.addRequest(req, onReorderComplete);
    }
  }

  public void reorderEnable(View unusedView) {
    Utils
        .log("SeasonPass::reorderEnable() " + Boolean.toString(mInReorderMode));

    final ArrayList<String> subscriptionIds = new ArrayList<String>();
    final ArrayList<Integer> slots = new ArrayList<Integer>();
    int i = 0;
    while (i < mSubscriptionData.size()) {
      if (mSubscriptionStatus.get(i) == SubscriptionStatus.MISSING) {
        String subscriptionId = mSubscriptionIds.get(i);
        subscriptionIds.add(subscriptionId);
        slots.add(i);
        mSubscriptionStatus.set(i, SubscriptionStatus.LOADING);
      }
      i++;
    }

    final ProgressDialog d = new ProgressDialog(this);
    final MindRpcResponseListener onAllPassesLoaded =
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            if (response != null) {
              mDetailCallback.onResponse(response);
            }
            d.dismiss();

            // Save the state before ordering.
            mSubscriptionIdsBeforeReorder.clear();
            mSubscriptionIdsBeforeReorder.addAll(mSubscriptionIds);
            // Flip the buttons.
            findViewById(R.id.reorder_enable).setVisibility(View.GONE);
            findViewById(R.id.reorder_apply).setVisibility(View.VISIBLE);
            // Show the drag handles.
            mInReorderMode = true;
            mListAdapter.notifyDataSetChanged();
          }
        };

    if (subscriptionIds.size() == 0) {
      // No subscriptions need loading? Proceed immediately.
      onAllPassesLoaded.onResponse(null);
    } else {
      // Otherwise, show dialog and start loading.
      d.setIndeterminate(true);
      d.setTitle("Preparing ...");
      d.setMessage("Loading all season pass data.");
      d.setCancelable(false);
      d.show();

      final SubscriptionSearch req = new SubscriptionSearch(subscriptionIds);
      mRequestSlotMap.put(req.getRpcId(), slots);
      MindRpc.addRequest(req, onAllPassesLoaded);
    }
  }
}
