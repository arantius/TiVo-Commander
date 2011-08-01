package com.arantius.tivocommander;

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

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
        new DownloadImageTask(iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("first")
          .getTextValue() + " " + item.path("last").getTextValue());
      if (item.has("characterName")) {
        ((TextView) v.findViewById(R.id.textView2)).setText("\""
            + item.path("characterName").getTextValue() + "\"");
      } else {
        v.findViewById(R.id.textView2).setVisibility(View.GONE);
      }
      // TODO: Format roles (i.e. not "executiveProducer").
      ((TextView) v.findViewById(R.id.textView3)).setText("("
          + item.path("role").getTextValue() + ")");

      return v;
    }
  }

  @Override
  protected BaseSearch getRequest() {
    return new CreditsSearch(mCollectionId, mContentId);
  }

  @Override
  protected void onContent() {
    JsonNode creditsNode = mContent.path("credit");
    JsonNode[] credits = new JsonNode[creditsNode.size()];
    int i = 0;
    for (JsonNode credit : creditsNode) {
      credits[i++] = credit;
    }

    setContentView(R.layout.list_explore);
    ListView lv = (ListView) findViewById(R.id.listView1);
    CreditsAdapter adapter =
        new CreditsAdapter(this, R.layout.item_credits, credits);
    lv.setAdapter(adapter);

    // TODO: Set on click listener for exploring actors.
  }
}
