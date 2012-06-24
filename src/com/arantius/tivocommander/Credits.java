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

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.BaseSearch;
import com.arantius.tivocommander.rpc.request.CreditsSearch;

public class Credits extends ExploreCommon {
  private class CreditsAdapter extends ArrayAdapter<JsonNode> {
    private final JsonNode[] mCredits;
    private final Drawable mPersonDrawable;

    public CreditsAdapter(Context context, int resource, JsonNode[] objects) {
      super(context, resource, objects);
      mCredits = objects;
      mPersonDrawable = context.getResources().getDrawable(R.drawable.person);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (v == null) {
        LayoutInflater vi =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.item_credits, null);
      }

      ImageView iv = (ImageView) v.findViewById(R.id.imageView1);
      View pv = v.findViewById(R.id.progressBar1);

      if (convertView != null) {
        iv.setImageDrawable(mPersonDrawable);
        pv.setVisibility(View.VISIBLE);
        v.findViewById(R.id.textView2).setVisibility(View.VISIBLE);
      }

      JsonNode item = mCredits[position];
      if (item == null) {
        Utils.log("Get view; item for position " + Integer.toString(position)
            + " is null");
        return null;
      }

      if (iv != null) {
        String imgUrl = Utils.findImageUrl(item);
        new DownloadImageTask(Credits.this, iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("first")
          .getTextValue() + " " + item.path("last").getTextValue());
      if (item.has("characterName")) {
        ((TextView) v.findViewById(R.id.textView2)).setText("\""
            + item.path("characterName").getTextValue() + "\"");
      } else {
        v.findViewById(R.id.textView2).setVisibility(View.GONE);
      }
      String role = Utils.ucFirst(item.path("role").getTextValue());
      role = role.replaceAll("(?=[A-Z])", " ").trim();
      ((TextView) v.findViewById(R.id.textView3)).setText(role);

      return v;
    }
  }

  private final OnItemClickListener mOnItemClickListener =
      new OnItemClickListener() {
        public void onItemClick(android.widget.AdapterView<?> parent,
            View view, int position, long id) {
          JsonNode person = mCredits.path(position);
          Intent intent = new Intent(getBaseContext(), Person.class);
          intent.putExtra("personId", person.path("personId").getTextValue());
          intent.putExtra("fName", person.path("first").getTextValue());
          intent.putExtra("lName", person.path("last").getTextValue());
          startActivity(intent);
        }
      };

  protected JsonNode mCredits;

  @Override
  protected BaseSearch getRequest() {
    return new CreditsSearch(mCollectionId, mContentId);
  }

  @Override
  protected void onContent() {
    getParent().setProgressBarIndeterminateVisibility(false);

    mCredits = mContent.path("credit");

    if (mCredits.size() == 0) {
      setContentView(R.layout.no_results);
    } else {
      setContentView(R.layout.list_explore);

      JsonNode[] credits = new JsonNode[mCredits.size()];
      int i = 0;
      for (JsonNode credit : mCredits) {
        credits[i++] = credit;
      }

      setContentView(R.layout.list_explore);
      ListView lv = (ListView) findViewById(R.id.listView1);
      CreditsAdapter adapter =
          new CreditsAdapter(this, R.layout.item_credits, credits);
      lv.setAdapter(adapter);
      lv.setOnItemClickListener(mOnItemClickListener);
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Utils.log(String.format("Credits: "
        + "contentId:%s collectionId:%s offerId:%s recordingId:%s", mContentId,
        mCollectionId, mOfferId, mRecordingId));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Credits");
    MindRpc.init(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Credits");
  }
}
