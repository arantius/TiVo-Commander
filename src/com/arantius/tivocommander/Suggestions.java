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
        new DownloadImageTask(iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("title")
          .getTextValue());

      return v;
    }
  }

  private final Activity mContext = this;
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
          setContentView(R.layout.list_explore);

          mShows =
              response.getBody().path("collection").path(0)
                  .path("correlatedCollectionForCollectionId");
          JsonNode[] shows = new JsonNode[mShows.size()];
          int i = 0;
          for (JsonNode show : mShows) {
            shows[i++] = show;
          }

          ListView lv = (ListView) findViewById(R.id.listView1);
          ShowAdapter adapter =
              new ShowAdapter(mContext, R.layout.item_show, shows);
          lv.setAdapter(adapter);
          lv.setOnItemClickListener(mOnItemClickListener);
        }
      };

  protected JsonNode mShows;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    setContentView(R.layout.progress);

    Bundle bundle = getIntent().getExtras();
    String collectionId;

    if (bundle != null) {
      collectionId = bundle.getString("collectionId");
      if (collectionId == null) {
        Toast.makeText(getApplicationContext(), "Oops; missing collection ID",
            Toast.LENGTH_SHORT).show();
      } else {
        SuggestionsSearch request = new SuggestionsSearch(collectionId);
        MindRpc.addRequest(request, mSuggestionListener);
      }
    }
  }
}
