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
