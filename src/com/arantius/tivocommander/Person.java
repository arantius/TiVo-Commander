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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
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
    private final Drawable mDrawable;
    private final int mResource;

    public CreditsAdapter(Context context, int resource, JsonNode[] objects) {
      super(context, resource, objects);
      mCredits = objects;
      mDrawable = context.getResources().getDrawable(R.drawable.content_banner);
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
        iv.setImageDrawable(mDrawable);
        pv.setVisibility(View.VISIBLE);
        v.findViewById(R.id.textView2).setVisibility(View.VISIBLE);
      }

      JsonNode item = mCredits[position];
      if (item == null) {
        return null;
      }

      if (iv != null) {
        String imgUrl = Utils.findImageUrl(item);
        new DownloadImageTask(Person.this, iv, pv).execute(imgUrl);
      }

      ((TextView) v.findViewById(R.id.textView1)).setText(item.path("title")
          .getTextValue());
      ((TextView) v.findViewById(R.id.textView2)).setText(Utils
          .ucFirst(findRole(item.path("credit"))));
      // TODO: Can we display / sort by the year?

      return v;
    }
  }

  private JsonNode mCredits = null;
  private String mName;
  private int mOutstandingRequests = 0;
  private final MindRpcResponseListener mPersonCreditsListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mCredits = response.getBody().path("collection");
          requestFinished();
        }
      };
  private final OnItemClickListener mOnItemClickListener =
      new OnItemClickListener() {
        public void onItemClick(android.widget.AdapterView<?> parent,
            View view, int position, long id) {
          JsonNode person = mCredits.path(position);
          Intent intent = new Intent(getBaseContext(), ExploreTabs.class);
          intent.putExtra("collectionId", person.path("collectionId")
              .getTextValue());
          startActivity(intent);
        }
      };
  private JsonNode mPerson = null;
  private String mPersonId;
  private final MindRpcResponseListener mPersonListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mPerson = response.getBody().path("person").path(0);
          requestFinished();
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    if (bundle == null) {
      Utils.log("Person: null bundle!");
      finish();
      return;
    }

    mName = bundle.getString("fName");
    if (bundle.getString("lName") != null) {
      mName += " " + bundle.getString("lName");
    }
    mPersonId = bundle.getString("personId");

    Utils.log(String.format("Person: " + "name:%s personId:%s", mName,
        mPersonId));

    MindRpc.init(this);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.list);

    MindRpc.addRequest(new PersonSearch(mPersonId), mPersonListener);
    mOutstandingRequests++;
    MindRpc.addRequest(new PersonCreditsSearch(mPersonId),
        mPersonCreditsListener);
    mOutstandingRequests++;

    setProgressBarIndeterminateVisibility(true);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Person");
    MindRpc.init(this);
  }

  private void requestFinished() {
    if (--mOutstandingRequests > 0) {
      return;
    }
    setProgressBarIndeterminateVisibility(false);

    if (mPerson == null || mCredits == null) {
      setContentView(R.layout.no_results);
      return;
    }

    setContentView(R.layout.list_person);

    // Credits.
    JsonNode[] credits = new JsonNode[mCredits.size()];
    int i = 0;
    for (JsonNode credit : mCredits) {
      credits[i++] = credit;
    }

    ListView lv = getListView();
    CreditsAdapter adapter =
        new CreditsAdapter(Person.this, R.layout.item_person_credits, credits);
    lv.setAdapter(adapter);
    lv.setOnItemClickListener(mOnItemClickListener);

    // Name.
    ((TextView) findViewById(R.id.person_name)).setText(mName);

    // Role.
    JsonNode rolesNode = mPerson.path("roleForPersonId");
    String[] roles = new String[rolesNode.size()];
    for (i = 0; i < rolesNode.size(); i++) {
      roles[i] = rolesNode.path(i).getTextValue();
      roles[i] = Utils.ucFirst(roles[i]);
    }
    ((TextView) findViewById(R.id.person_role))
        .setText(Utils.join(", ", roles));

    // Birth date.
    TextView birthdateView = ((TextView) findViewById(R.id.person_birthdate));
    if (mPerson.has("birthDate")) {
      Date birthdate =
          Utils.parseDateStr(mPerson.path("birthDate").getTextValue());
      SimpleDateFormat dateFormatter = new SimpleDateFormat("MMMMM d, yyyy");
      dateFormatter.setTimeZone(TimeZone.getDefault());
      Spannable birthdateStr =
          new SpannableString("Birthdate: " + dateFormatter.format(birthdate));
      birthdateStr.setSpan(new ForegroundColorSpan(Color.WHITE), 11,
          birthdateStr.length(), 0);
      birthdateView.setText(birthdateStr);
    } else {
      birthdateView.setVisibility(View.GONE);
    }

    // Birth place.
    TextView birthplaceView = ((TextView) findViewById(R.id.person_birthplace));
    if (mPerson.has("birthPlace")) {
      Spannable birthplaceStr =
          new SpannableString("Birthplace: "
              + mPerson.path("birthPlace").getTextValue());
      birthplaceStr.setSpan(new ForegroundColorSpan(Color.WHITE), 12,
          birthplaceStr.length(), 0);
      birthplaceView.setText(birthplaceStr);
    } else {
      birthplaceView.setVisibility(View.GONE);
    }

    ImageView iv = (ImageView) findViewById(R.id.imageView1);
    View pv = findViewById(R.id.progressBar1);
    String imgUrl = Utils.findImageUrl(mPerson);
    new DownloadImageTask(this, iv, pv).execute(imgUrl);
  };

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
