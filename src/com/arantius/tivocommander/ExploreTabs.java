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

import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.arantius.tivocommander.rpc.MindRpc;

public class ExploreTabs extends TabActivity {
  private String mCollectionId;
  private String mContentId;
  private TabHost mTabHost;

  private TabSpec makeTab(String name, Class<? extends Activity> cls) {
    TabSpec tab = mTabHost.newTabSpec(name);
    tab.setIndicator(name);

    Intent intent = new Intent(getBaseContext(), cls);
    if (mCollectionId != null) {
      intent.putExtra("collectionId", mCollectionId);
    }
    if (mContentId != null) {
      intent.putExtra("contentId", mContentId);
    }
    tab.setContent(intent);

    return tab;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    setContentView(R.layout.explore_tabs);
    mTabHost = getTabHost();
    Bundle bundle = getIntent().getExtras();
    try {
      mCollectionId = bundle.getString("collectionId");
    } catch (NullPointerException e) {
      mCollectionId = null;
    }
    try {
      mContentId = bundle.getString("contentId");
    } catch (NullPointerException e) {
      mContentId = null;
    }

    mTabHost.addTab(makeTab("Explore", Explore.class));
    mTabHost.addTab(makeTab("Credits", Credits.class));
    mTabHost.addTab(makeTab("Also", Suggestions.class));
  }
}
