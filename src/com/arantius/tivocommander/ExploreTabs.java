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

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.arantius.tivocommander.rpc.MindRpc;

@SuppressWarnings("deprecation")
public class ExploreTabs extends TabActivity {
  private String mCollectionId;
  private String mContentId;
  private String mOfferId;
  private String mRecordingId;
  private TabHost mTabHost;

  private TabSpec makeTab(String name, Class<? extends Activity> cls, int iconId) {
    TabSpec tab = mTabHost.newTabSpec(name);
    tab.setIndicator(name, getResources().getDrawable(iconId));

    Intent intent = new Intent(getBaseContext(), cls);
    if (mCollectionId != null) {
      intent.putExtra("collectionId", mCollectionId);
    }
    if (mContentId != null) {
      intent.putExtra("contentId", mContentId);
    }
    if (mOfferId != null) {
      intent.putExtra("offerId", mOfferId);
    }
    if (mRecordingId != null) {
      intent.putExtra("recordingId", mRecordingId);
    }
    tab.setContent(intent);

    return tab;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle bundle = getIntent().getExtras();

    // Put the URI details, if any, in the bundle. Where we'll read them
    // later, even if MindRpc.init() restarts us with only bundle data.
    Uri uri = getIntent().getData();
    if (uri != null) {
      uriToBundle(uri, bundle);
    }

    if (MindRpc.init(this, bundle)) {
      return;
    }

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setContentView(R.layout.explore_tabs);
    setTitle("Explore");

    mTabHost = getTabHost();

    try {
      mCollectionId = bundle.getString("collectionId");
      if ("tivo:cl.0".equals(mCollectionId)) {
        mCollectionId = null;
      }
    } catch (NullPointerException e) {
      mCollectionId = null;
    }
    try {
      mContentId = bundle.getString("contentId");
    } catch (NullPointerException e) {
      mContentId = null;
    }
    try {
      mOfferId = bundle.getString("offerId");
    } catch (NullPointerException e) {
      mOfferId = null;
    }
    try {
      mRecordingId = bundle.getString("recordingId");
    } catch (NullPointerException e) {
      mRecordingId = null;
    }

    mTabHost.addTab(makeTab("Explore", Explore.class, R.drawable.icon_tv));
    if (mCollectionId != null) {
      mTabHost
          .addTab(makeTab("Credits", Credits.class, R.drawable.icon_people));
      mTabHost.addTab(makeTab("Similar", Suggestions.class,
          R.drawable.icon_similar));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    return Utils.onCreateOptionsMenu(menu, this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  /** Parse a TiVo URL in a Uri object into an extras bundle. */
  private void uriToBundle(Uri uri, Bundle bundle) {
    final String[] keys = new String[] {
        "collectionId", "contentId", "offerId",
    };
    for (String key : keys) {
      String val = uri.getQueryParameter(key);
      if (val != null) {
        bundle.putString(key, val);
      }
    }
  }
}
