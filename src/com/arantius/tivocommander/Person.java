package com.arantius.tivocommander;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.PersonCreditsSearch;
import com.arantius.tivocommander.rpc.request.PersonSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Person extends ListActivity {
  // TODO: Refactor this to be DRY w/ Credits.
  private class CreditsAdapter extends ArrayAdapter<JsonNode> {
    private final JsonNode[] mCredits;
    private final Drawable mPersonDrawable;
    private final int mResource;

    public CreditsAdapter(Context context, int resource, JsonNode[] objects) {
      super(context, resource, objects);
      mCredits = objects;
      mPersonDrawable = context.getResources().getDrawable(R.drawable.person);
      mResource = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;

      if (v == null) {
        LayoutInflater vi =
            (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = vi.inflate(mResource, null);
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
        return null;
      }

      if (iv != null) {
        String imgUrl = Utils.findImageUrl(item);
        new DownloadImageTask(iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("title")
          .getTextValue());
      ((TextView) v.findViewById(R.id.textView2)).setText(Utils
          .ucFirst(findRole(item.path("credit"))));

      return v;
    }
  }

  private Activity mContext;
  private JsonNode mCredits;
  private String mName;
  private final MindRpcResponseListener mPersonCreditsListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mCredits = response.getBody().path("collection");
          JsonNode[] credits = new JsonNode[mCredits.size()];
          int i = 0;
          for (JsonNode credit : mCredits) {
            credits[i++] = credit;
          }

          CreditsAdapter adapter =
              new CreditsAdapter(mContext, R.layout.item_person_credits,
                  credits);
          getListView().setAdapter(adapter);
        }
      };
  private String mPersonId;
  private final MindRpcResponseListener mPersonListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setContentView(R.layout.list_person);
          JsonNode person = response.getBody().path("person").path(0);

          // Name.
          ((TextView) findViewById(R.id.person_name)).setText(mName);

          // Role.
          JsonNode rolesNode = person.path("roleForPersonId");
          String[] roles = new String[rolesNode.size()];
          for (int i = 0; i < rolesNode.size(); i++) {
            roles[i] = rolesNode.path(i).getTextValue();
            roles[i] = Utils.ucFirst(roles[i]);
          }
          ((TextView) findViewById(R.id.person_role)).setText(Utils.join(", ",
              roles));

          // Birth date.
          TextView birthdateView =
              ((TextView) findViewById(R.id.person_birthdate));
          if (person.has("birthDate")) {
            SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-mm-dd");
            ParsePosition pp = new ParsePosition(0);
            Date birthdate =
                dateParser.parse(person.path("birthDate").getTextValue(), pp);
            SimpleDateFormat dateFormatter =
                new SimpleDateFormat("MMMMM d, yyyy");
            Spannable birthdateStr =
                new SpannableString("Birthdate: "
                    + dateFormatter.format(birthdate));
            birthdateStr.setSpan(new ForegroundColorSpan(Color.WHITE), 11,
                birthdateStr.length(), 0);
            birthdateView.setText(birthdateStr);
          } else {
            birthdateView.setVisibility(View.GONE);
          }

          // Birth place.
          TextView birthplaceView =
              ((TextView) findViewById(R.id.person_birthplace));
          if (person.has("birthPlace")) {
            Spannable birthplaceStr =
                new SpannableString("Birthplace: "
                    + person.path("birthPlace").getTextValue());
            birthplaceStr.setSpan(new ForegroundColorSpan(Color.WHITE), 12,
                birthplaceStr.length(), 0);
            birthplaceView.setText(birthplaceStr);
          } else {
            birthplaceView.setVisibility(View.GONE);
          }

          ImageView iv = (ImageView) findViewById(R.id.imageView1);
          View pv = findViewById(R.id.progressBar1);
          String imgUrl = Utils.findImageUrl(person);
          new DownloadImageTask(iv, pv).execute(imgUrl);
        }
      };

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle == null) {
      finish();
      return;
    }

    mContext = this;
    mName =
        String.format("%s %s", bundle.getString("fName"),
            bundle.getString("lName"));
    mPersonId = bundle.getString("personId");

    setTitle("TiVo Commander - " + mName);
    MindRpc.addRequest(new PersonSearch(mPersonId), mPersonListener);
    MindRpc.addRequest(new PersonCreditsSearch(mPersonId),
        mPersonCreditsListener);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }

  private String findRole(JsonNode credits) {
    for (JsonNode credit : credits) {
      String x = credit.path("personId").getTextValue();
      if (mPersonId.equals(x)) {
        return credit.path("role").getTextValue();
      }
    }
    return null;
  }
}
