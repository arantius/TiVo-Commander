package com.arantius.tivocommander;

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Credits extends ExploreCommon {
  private class CreditsAdapter extends ArrayAdapter<JsonNode> {
    private final ArrayList<JsonNode> mCredits;

    public CreditsAdapter(Context context, int resource,
        ArrayList<JsonNode> objects) {
      super(context, resource, objects);
      mCredits = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      if (v == null) {
        LayoutInflater vi =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(R.layout.credits_item, null);
      }

      JsonNode item = mCredits.get(position);
      if (item != null) {
        ImageView iv = (ImageView) v.findViewById(R.id.imageView1);
        View pv = v.findViewById(R.id.progressBar1);
        if (iv != null) {
          new DownloadImageTask(iv, pv);
        }

        ((TextView) v.findViewById(R.id.textView1)).setText(item.path("first")
            .getTextValue() + " " + item.path("last").getTextValue());
        ((TextView) v.findViewById(R.id.textView2)).setText("\""
            + item.path("characterName").getTextValue() + "\"");
        ((TextView) v.findViewById(R.id.textView3)).setText("("
            + item.path("role").getTextValue() + ")");
      }

      return v;
    }
  }

  @Override
  protected void onContent() {
    ArrayList<JsonNode> credits = new ArrayList<JsonNode>();
    JsonNode creditsNode = mContent.path("credit");
    for (int i = 0; i < creditsNode.size(); i++) {
      credits.add(creditsNode.path(i));
    }

    setContentView(R.layout.credits);
    ListView lv = (ListView) findViewById(R.id.listView1);
    CreditsAdapter adapter =
        new CreditsAdapter(this, R.layout.credits_item, credits);
    lv.setAdapter(adapter);
  }
}
