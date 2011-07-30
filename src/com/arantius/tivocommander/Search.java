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
            String title = result.path("title").getTextValue();
            if (title != null) {
              mResultTitles.add(title);
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
          Intent intent = new Intent(getBaseContext(), ExploreTabs.class);
          intent.putExtra("collectionId",
              mResults.path(position).path("collectionId").getTextValue());
          startActivity(intent);
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
