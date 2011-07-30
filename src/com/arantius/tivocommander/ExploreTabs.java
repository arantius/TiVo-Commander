package com.arantius.tivocommander;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

import com.arantius.tivocommander.rpc.MindRpc;

public class ExploreTabs extends TabActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    setContentView(R.layout.explore_tabs);
    TabHost tabHost = getTabHost();
    Bundle bundle = getIntent().getExtras();

    TabSpec exploreTab = tabHost.newTabSpec("Explore");
    exploreTab.setIndicator("Explore");
    Intent exploreIntent = new Intent(getBaseContext(), Explore.class);
    exploreIntent.putExtra("contentId", bundle.getString("contentId"));
    exploreIntent.putExtra("collectionId", bundle.getString("collectionId"));
    exploreTab.setContent(exploreIntent);
    tabHost.addTab(exploreTab);
  }
}
