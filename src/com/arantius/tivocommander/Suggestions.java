package com.arantius.tivocommander;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.SuggestionsSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Suggestions extends Activity {
  private class ShowAdapter extends ArrayAdapter<JsonNode> {
    private final JsonNode[] mShows;

    private final Drawable mDrawable;

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

  private final MindRpcResponseListener mSuggestionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setContentView(R.layout.list_explore);

          JsonNode showsNode =
              response.getBody().path("collection").path(0)
                  .path("correlatedCollectionForCollectionId");
          JsonNode[] shows = new JsonNode[showsNode.size()];
          int i = 0;
          for (JsonNode show : showsNode) {
            shows[i++] = show;
          }

          ListView lv = (ListView) findViewById(R.id.listView1);
          ShowAdapter adapter =
              new ShowAdapter(mContext, R.layout.item_show, shows);
          lv.setAdapter(adapter);
        }
      };

  private final Activity mContext = this;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    setContentView(R.layout.progress);

    Bundle bundle = getIntent().getExtras();
    String collectionId;

    // TODO: On click listener!

    if (bundle != null) {
      collectionId = bundle.getString("collectionId");
      SuggestionsSearch request = new SuggestionsSearch(collectionId);
      MindRpc.addRequest(request, mSuggestionListener);
    }
  }
}
