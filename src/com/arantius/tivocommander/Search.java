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

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.CancelRpc;
import com.arantius.tivocommander.rpc.request.UnifiedItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

// TODO: Still get some strange errors when issuing many searches.
// TODO: What happened to people results?

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

      ImageView iv = (ImageView) v.findViewById(R.id.imageView1);
      View pv = v.findViewById(R.id.progressBar1);

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
        title = item.path("title").getTextValue();
      } else if (item.has("personId")) {
        // TODO: Handle missing last name (e.g. Oprah).
        title = item.path("first").getTextValue();
        if (item.has("last")) {
          // Some people only have one (thus first) name.
          title += " " + item.path("last").getTextValue();
        }
      } else {
        Utils.log("Could not find title!");
        Utils.log(item.toString());
        return v;
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(title);

      return v;
    }
  }

  private final class SearchTask extends AsyncTask<String, Void, Void> {
    @Override
    protected Void doInBackground(String... params) {
      // Give the user time to type more.
      try {
        Thread.sleep(250);
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

      mRequest = new UnifiedItemSearch(params[0] + "*");
      MindRpc.addRequest(mRequest, mSearchListener);
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
            intent.putExtra("contentId", result.path("contentId")
                .getTextValue());
            intent.putExtra("collectionId", result.path("collectionId")
                .getTextValue());
            startActivity(intent);
          } else if (result.has("personId")) {
            Intent intent = new Intent(getBaseContext(), Person.class);
            intent.putExtra("fName", result.path("first").getTextValue());
            intent.putExtra("lName", result.path("last").getTextValue());
            intent.putExtra("personId", result.path("personId").getTextValue());
            startActivity(intent);
          } else {
            Utils
                .logError("Result had neither collectionId, contentId, nor personId!\n"
                    + Utils.stringifyToJson(result));
          }
        }
      };
  private UnifiedItemSearch mRequest = null;
  private final ArrayList<JsonNode> mResults = new ArrayList<JsonNode>();

  private final MindRpcResponseListener mSearchListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          if (mRequest != null) {
            if (response.getRpcId() != mRequest.getRpcId()) {
              Utils.log("Got response for non-current search request!");
              return;
            }
          }
          mRequest = null;
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
      if (mRequest != null) {
        MindRpc.addRequest(new CancelRpc(mRequest.getRpcId()), null);
        mRequest = null;
      }

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
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.search);

    final EditText searchBox = (EditText) findViewById(R.id.search_box);
    mAdapter = new SearchAdapter(this, R.layout.item_show, mResults);

    setListAdapter(mAdapter);
    searchBox.addTextChangedListener(mTextWatcher);

    final ListView lv = getListView();
    lv.setOnItemClickListener(mOnClickListener);

    mEmptyView = findViewById(android.R.id.empty);
    mEmptyView.setVisibility(View.INVISIBLE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
