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

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.UnifiedItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;

public class Search extends ListActivity {
  private class SearchAdapter extends ArrayAdapter<JsonNode> {
    // TODO: Make this class DRY vs. Suggestions.ShowAdapter
    private final Drawable mDrawable;

    private final ArrayList<JsonNode> mItems;

    public SearchAdapter(Context context, int resource,
        ArrayList<JsonNode> objects) {
      super(context, resource, objects);
      mItems = objects;
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

      ImageView iv = (ImageView) v.findViewById(R.id.image_show);
      View pv = v.findViewById(R.id.image_show_progress);

      if (convertView != null) {
        iv.setImageDrawable(mDrawable);
        pv.setVisibility(View.VISIBLE);
      }

      JsonNode item = mItems.get(position);
      if (item == null) {
        return null;
      }

      if (iv != null) {
        String imgUrl = Utils.findImageUrl(item);
        if (item.has("personId")) {
          iv.setImageResource(R.drawable.person);
        }
        new DownloadImageTask(Search.this, iv, pv).execute(imgUrl);
      }

      String title = null;
      if (item.has("collectionId") || item.has("contentId")) {
        title = item.path("title").asText();
      } else if (item.has("personId")) {
        title = item.path("first").asText();
        if (item.has("last")) {
          // Some people only have one (thus first) name.
          title += " " + item.path("last").asText();
        }
      } else {
        Utils.log("Could not find title!");
        Utils.log(item.toString());
        return v;
      }

      ((TextView) v.findViewById(R.id.show_name)).setText(title);

      return v;
    }
  }

  private final class SearchTask extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {
      // Give the user time to type more.
      try {
        Thread.sleep(333);
      } catch (InterruptedException e) {
        // No-op.
      }

      // Then proceed.
      if (isCancelled()) {
        return null;
      }
      runOnUiThread(new Runnable() {
        public void run() {
          setProgressBarIndeterminateVisibility(true);
        }
      });

      UnifiedItemSearch request = new UnifiedItemSearch(params[0] + "*");
      MindRpc.addRequest(request, mSearchListener);
      return null;
    }
  }

  private SearchAdapter mAdapter;
  private View mEmptyView;
  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode result = mResults.get(position);
          if (result.has("collectionId") || result.has("contentId")) {
            Intent intent = new Intent(getBaseContext(), ExploreTabs.class);
            if (result.has("collectionId")) {
              intent.putExtra("collectionId", result.path("collectionId")
                  .asText());
            }
            if (result.has("contentId")) {
              intent.putExtra("contentId", result.path("contentId")
                  .asText());
            }
            startActivity(intent);
          } else if (result.has("personId")) {
            Intent intent = new Intent(getBaseContext(), Person.class);
            intent.putExtra("fName", result.path("first").asText());
            intent.putExtra("lName", result.path("last").asText());
            intent.putExtra("personId", result.path("personId").asText());
            startActivity(intent);
          } else {
            Utils
                .logError("Result had neither collectionId, contentId, nor personId!\n"
                    + Utils.stringifyToJson(result));
          }
        }
      };
  private final ArrayList<JsonNode> mResults = new ArrayList<JsonNode>();

  private final MindRpcResponseListener mSearchListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mEmptyView.setVisibility(View.VISIBLE);

          JsonNode resultsNode = response.getBody().path("unifiedItem");
          mResults.clear();
          for (JsonNode result : resultsNode) {
            mResults.add(result);
          }
          mAdapter.notifyDataSetChanged();

          setProgressBarIndeterminateVisibility(false);
        }
      };

  private AsyncTask<String, Void, Void> mSearchTask = null;

  private final TextWatcher mTextWatcher = new TextWatcher() {
    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
      // Cancel any previous request.
      if (mSearchTask != null) {
        mSearchTask.cancel(true);
        mSearchTask = null;
      }
      MindRpc.cancelAll();

      // Handle empty input.
      if ("".equals(s.toString())) {
        mResults.clear();
        runOnUiThread(new Runnable() {
          public void run() {
            mAdapter.notifyDataSetChanged();
            mEmptyView.setVisibility(View.INVISIBLE);
          }
        });
        return;
      }

      mSearchTask = new SearchTask().execute(s.toString());
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.search);
    setTitle("Search");

    final EditText searchBox = (EditText) findViewById(R.id.search_box);
    getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

    mAdapter = new SearchAdapter(this, R.layout.item_show, mResults);

    setListAdapter(mAdapter);
    searchBox.addTextChangedListener(mTextWatcher);

    final ListView lv = getListView();
    lv.setOnItemClickListener(mOnClickListener);

    mEmptyView = findViewById(android.R.id.empty);
    mEmptyView.setVisibility(View.INVISIBLE);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    Utils.createFullOptionsMenu(menu, this);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Search");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Search");
    MindRpc.init(this, null);
  }
}
