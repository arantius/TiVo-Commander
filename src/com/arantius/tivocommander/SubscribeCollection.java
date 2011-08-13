package com.arantius.tivocommander;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.codehaus.jackson.JsonNode;

import android.os.Bundle;
import android.view.View;
import android.widget.SimpleAdapter;
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
  private final int mPriority = 0;

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

    request.setPriority(mPriority);

    subscribeRequestCommon(request);

    setProgressBarIndeterminateVisibility(true);
    MindRpc.addRequest(request, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        if (response.getBody().has("conflicts")) {
          handleConflicts(response.getBody().path("conflicts"));
        } else if (response.getBody().has("subscription")) {
          finish();
        } else {
          Utils.log("What kind of subscribe response is this??");
          Utils.log(Utils.stringifyToPrettyJson(response.getBody()));
        }
      }
    });
  }

  private void handleConflicts(JsonNode conflicts) {
    setProgressBarIndeterminateVisibility(false);
    setContentView(R.layout.subscribe_conflicts);

    // "Will Record" list.
    ArrayList<HashMap<String, String>> willRecord =
        new ArrayList<HashMap<String, String>>();
    for (JsonNode conflict : conflicts.path("willGet")) {
      HashMap<String, String> listItem = conflictListItem(conflict, false);
      willRecord.add(listItem);
    }
    LinearListView willLv = (LinearListView) findViewById(R.id.will_record);
    willLv.setAdapter(new SimpleAdapter(this, willRecord,
        R.layout.item_sub_base, new String[] { "channel", "show_name",
            "show_time" }, new int[] { R.id.channel, R.id.show_name,
            R.id.show_time }));

    // "Will NOT Record" list.
    ArrayList<HashMap<String, String>> willNotRecord =
        new ArrayList<HashMap<String, String>>();
    for (JsonNode conflict : conflicts.path("wontGet")) {
      HashMap<String, String> listItem =
          conflictListItem(conflict.path("losingOffer").path(0), false);
      listItem.putAll(conflictListItem(conflict.path("winningOffer").path(0),
          true));
      willNotRecord.add(listItem);
    }
    LinearListView willNotLv = (LinearListView) findViewById(R.id.wont_record);
    willNotLv.setAdapter(new SimpleAdapter(this, willNotRecord,
        R.layout.item_sub_wont, new String[] { "channel", "overlap_show_name",
            "overlap_show_time", "show_name", "show_time" }, new int[] {
            R.id.channel, R.id.overlap_show_name, R.id.overlap_show_time,
            R.id.show_name, R.id.show_time }));
  }

  private HashMap<String, String> conflictListItem(JsonNode conflict,
      boolean overlapMode) {
    String prefix = overlapMode ? "overlap_" : "";
    HashMap<String, String> listItem = new HashMap<String, String>();
    listItem.put(prefix + "show_name",
        conflict.path(overlapMode ? "title" : "subtitle").getTextValue());
    listItem.put(prefix + "channel",
        conflict.path("channel").path("channelNumber").getTextValue() + " "
            + conflict.path("channel").path("callSign").getTextValue());

    // TODO: UTC -> local.
    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    ParsePosition pp = new ParsePosition(0);
    Date startTime =
        dateParser.parse(conflict.path("startTime").getTextValue(), pp);
    SimpleDateFormat dateFormatter1 =
        new SimpleDateFormat(overlapMode ? "hh:mm - " : "MMM dd  hh:mm - ");
    String showTime = dateFormatter1.format(startTime);
    Calendar endTime = Calendar.getInstance();
    endTime.setTime(startTime);
    endTime.add(Calendar.SECOND, conflict.path("duration").getIntValue());
    SimpleDateFormat dateFormatter2 = new SimpleDateFormat("hh:mm a");
    showTime += dateFormatter2.format(endTime.getTime());
    listItem.put(prefix + "show_time", showTime);

    return listItem;
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
