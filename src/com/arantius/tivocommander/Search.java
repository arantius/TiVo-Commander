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
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.CancelRpc;
import com.arantius.tivocommander.rpc.request.UnifiedItemSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Search extends ListActivity {
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

      if ("".equals(params[0])) {
        runOnUiThread(new Runnable() {
          public void run() {
            mResults = null;
            mResultTitles.clear();
            mAdapter.notifyDataSetChanged();
          }
        });
      }

      if (mRequest != null) {
        MindRpc.addRequest(new CancelRpc(mRequest.getRpcId()), null);
      }
      mRequest = new UnifiedItemSearch(params[0] + "*");
      // TODO: Progress indicator.
      MindRpc.addRequest(mRequest, mSearchListener);
      return null;
    }
  }

  private ArrayAdapter<String> mAdapter;
  private UnifiedItemSearch mRequest = null;
  private JsonNode mResults = null;
  private AsyncTask<String, Void, Void> mSearchTask = null;
  private final ArrayList<String> mResultTitles = new ArrayList<String>();

  private final TextWatcher mTextWatcher = new TextWatcher() {
    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (mSearchTask != null) {
        mSearchTask.cancel(true);
      }

      // TODO: Handle empty query.

      mSearchTask = new SearchTask().execute(s.toString());
    }
  };

  private final MindRpcResponseListener mSearchListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mRequest = null;
          mResults = response.getBody().path("unifiedItem");

          // TODO: Handle zero results.

          mResultTitles.clear();
          for (int i = 0; i < mResults.size(); i++) {
            final JsonNode result = mResults.path(i);
            if (result.has("collectionId") || result.has("contentId")) {
              mResultTitles.add(result.path("title").getTextValue());
            } else if (result.has("personId")) {
              mResultTitles.add(result.path("first").getTextValue() + " "
                  + result.path("last").getTextValue());
            } else {
              Utils.log("Could not find title!");
              Utils.log(result.toString());
            }
          }

          mAdapter.notifyDataSetChanged();
        }
      };

  private final OnItemClickListener mOnClickListener =
      new OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
          JsonNode result = mResults.path(position);
          if (result.has("collectionId") || result.has("contentId")) {
            Intent intent = new Intent(getBaseContext(), ExploreTabs.class);
            intent.putExtra("contentId", result.path("contentId")
                .getTextValue());
            intent.putExtra("collectionId", result.path("collectionId")
                .getTextValue());
            startActivity(intent);
          } else if (result.has("personId")) {
            Intent intent = new Intent(getBaseContext(), Person.class);
            intent.putExtra("personId", result.path("personId").getTextValue());
            startActivity(intent);
          } else {
            Utils
                .logError("Result had neither collectionId, contentId, nor personId!\n"
                    + Utils.stringifyToJson(result));
          }
        }
      };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("TiVo Commander - Search");
    setContentView(R.layout.search);

    final EditText searchBox = (EditText) findViewById(R.id.search_box);
    mAdapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
            mResultTitles);

    setListAdapter(mAdapter);
    searchBox.addTextChangedListener(mTextWatcher);

    final ListView lv = getListView();
    lv.setOnItemClickListener(mOnClickListener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
