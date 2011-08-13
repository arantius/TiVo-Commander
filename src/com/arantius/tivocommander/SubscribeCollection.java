package com.arantius.tivocommander;

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.OfferSearch;
import com.arantius.tivocommander.rpc.request.Subscribe;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class SubscribeCollection extends SubscribeBase {
  private final static String[] mMaxLabels =
      new String[] { "1 recorded show", "2 recorded shows", "3 recorded shows",
          "4 recorded shows", "5 recorded shows", "10 recorded shows",
          "25 recorded shows", "All shows" };
  private final static Integer[] mMaxValues = new Integer[] { 1, 2, 3, 4, 5,
      10, 25, null };

  private final static String[] mWhichLabels = new String[] {
      "Repeats & first-run", "First-run only", "All (with duplicates)" };
  private final static String[] mWhichValues = new String[] { "rerunsAllowed",
      "firstRunOnly", "everyEpisode" };
  private String[] mChannelNames;
  private final ArrayList<JsonNode> mChannelNodes = new ArrayList<JsonNode>();
  private final MindRpcResponseListener mChannelsListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode offers = response.getBody().path("offerGroup");
          mChannelNodes.clear();
          mChannelNames = new String[offers.size()];

          int i = 0;
          for (JsonNode offer : offers) {
            JsonNode channel = offer.path("example").path("channel");
            mChannelNodes.add(channel);
            mChannelNames[i++] =
                channel.path("channelNumber").getTextValue() + " "
                    + channel.path("callSign").getTextValue();
          }

          setContentView(R.layout.subscribe_collection);
          setUpSpinner(R.id.channel, mChannelNames);
          setUpSpinner(R.id.record_which, mWhichLabels);
          setUpSpinner(R.id.record_max, mMaxLabels);
          setUpSpinner(R.id.duration, mKeepLabels);
          setUpSpinner(R.id.start, mStartLabels);
          setUpSpinner(R.id.stop, mStopLabels);
        }
      };
  private String mCollectionId;

  public void doSubscribe(View v) {
    Subscribe request = new Subscribe();

    int channelPos =
        ((Spinner) findViewById(R.id.channel)).getSelectedItemPosition();
    int maxPos =
        ((Spinner) findViewById(R.id.record_max)).getSelectedItemPosition();
    int whichPos =
        ((Spinner) findViewById(R.id.record_which)).getSelectedItemPosition();
    request.setCollection(mCollectionId, mChannelNodes.get(channelPos),
        mMaxValues[maxPos], mWhichValues[whichPos]);

    subscribeRequestCommon(request);

    setProgressBarIndeterminateVisibility(true);
    MindRpc.addRequest(request, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        Utils.log(Utils.stringifyToPrettyJson(response.getBody()));
        finish();
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    mCollectionId = bundle.getString("collectionId");

    OfferSearch request = new OfferSearch();
    request.setChannelsForCollection(mCollectionId);
    MindRpc.addRequest(request, mChannelsListener);
  }
}
