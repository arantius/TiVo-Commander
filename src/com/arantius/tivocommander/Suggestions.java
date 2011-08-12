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

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
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
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.SuggestionsSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Suggestions extends Activity {
  private class ShowAdapter extends ArrayAdapter<JsonNode> {
    private final Drawable mDrawable;

    private final JsonNode[] mShows;

    public ShowAdapter(Context context, int resource, JsonNode[] objects) {
      super(context, resource, objects);
      mShows = objects;
      mDrawable = context.getResources().getDrawable(R.drawable.content_banner);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (v == null) {
        LayoutInflater vi =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.item_show, null);
      }

      ImageView iv = (ImageView) v.findViewById(R.id.imageView1);
      View pv = v.findViewById(R.id.progressBar1);

      if (convertView != null) {
        iv.setImageDrawable(mDrawable);
        pv.setVisibility(View.VISIBLE);
      }

      JsonNode item = mShows[position];
      if (item == null) {
        return null;
      }

      if (iv != null) {
        String imgUrl = Utils.findImageUrl(item);
        new DownloadImageTask(Suggestions.this, iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("title")
          .getTextValue());

      return v;
    }
  }

  private final OnItemClickListener mOnItemClickListener =
      new OnItemClickListener() {
        public void onItemClick(android.widget.AdapterView<?> parent,
            View view, int position, long id) {
          String collectionId =
              mShows.path(position).path("collectionId").getTextValue();
          Intent intent = new Intent(getBaseContext(), ExploreTabs.class);
          intent.putExtra("collectionId", collectionId);
          startActivity(intent);
        }
      };

  private final MindRpcResponseListener mSuggestionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          getParent().setProgressBarIndeterminateVisibility(false);
          mShows =
              response.getBody().path("collection").path(0)
                  .path("correlatedCollectionForCollectionId");

          if (mShows.size() == 0) {
            setContentView(R.layout.no_results);
          } else {
            setContentView(R.layout.list_explore);

            JsonNode[] shows = new JsonNode[mShows.size()];
            int i = 0;
            for (JsonNode show : mShows) {
              shows[i++] = show;
            }

            ListView lv = (ListView) findViewById(R.id.listView1);
            ShowAdapter adapter =
                new ShowAdapter(Suggestions.this, R.layout.item_show, shows);
            lv.setAdapter(adapter);
            lv.setOnItemClickListener(mOnItemClickListener);
          }
        }
      };

  protected JsonNode mShows;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    String collectionId;

    if (bundle != null) {
      collectionId = bundle.getString("collectionId");
      if (collectionId == null) {
        Toast.makeText(getApplicationContext(), "Oops; missing collection ID",
            Toast.LENGTH_SHORT).show();
      } else {
        getParent().setProgressBarIndeterminateVisibility(true);
        SuggestionsSearch request = new SuggestionsSearch(collectionId);
        MindRpc.addRequest(request, mSuggestionListener);
      }
    }
  }
}
