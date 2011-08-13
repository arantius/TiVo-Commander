package com.arantius.tivocommander;

import android.os.Bundle;
import android.view.View;

import com.arantius.tivocommander.rpc.request.Subscribe;

public class SubscribeCollection extends SubscribeBase {
  private final static String[] mWhichValues = new String[] { "rerunsAllowed",
      "firstRunOnly", "everyEpisode" };
  private final static String[] mWhichLabels = new String[] {
      "Repeats & first-run", "First-run only", "All (with duplicates)" };

  private final static Integer[] mMaxValues = new Integer[] { 1, 2, 3, 4, 5,
      10, 25, null };
  private final static String[] mMaxLabels =
      new String[] { "1 recorded show", "2 recorded shows", "3 recorded shows",
          "4 recorded shows", "5 recorded shows", "10 recorded shows",
          "25 recorded shows", "All shows" };

  public void doSubscribe(View v) {
    Bundle bundle = getIntent().getExtras();

    Subscribe request = new Subscribe();
//    request.setCollection(bundle.getString("collectionId"));
    subscribeRequestCommon(request);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.subscribe_collection);
//    setUpSpinner(R.id.channel, mKeepLabels);
    setUpSpinner(R.id.record_which, mWhichLabels);
    setUpSpinner(R.id.record_max, mMaxLabels);
    setUpSpinner(R.id.duration, mKeepLabels);
    setUpSpinner(R.id.start, mStartLabels);
    setUpSpinner(R.id.stop, mStopLabels);
  }
}
