package com.arantius.tivocommander;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
      10, 25, 0 };

  private final static String[] mWhichLabels = new String[] {
      "Repeats & first-run", "First-run only", "All (with duplicates)" };
  private final static String[] mWhichValues = new String[] { "rerunsAllowed",
      "firstRunOnly", "everyEpisode" };
  private JsonNode mChannel;
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
          if (i == 0) {
            Toast.makeText(getApplicationContext(),
                "Sorry: Couldn't find any channels to record that on.",
                Toast.LENGTH_SHORT).show();
            finish();
            return;
          }

          setContentView(R.layout.subscribe_collection);
          setUpSpinner(R.id.channel, mChannelNames);
          setUpSpinner(R.id.record_which, mWhichLabels);
          setUpSpinner(R.id.record_max, mMaxLabels);
          setUpSpinner(R.id.until, mUntilLabels);
          setUpSpinner(R.id.start, mStartLabels);
          setUpSpinner(R.id.stop, mStopLabels);

          // Set defaults.
          ((Spinner) findViewById(R.id.record_max)).setSelection(4);

          // If known, set values from existing subscription.
          if (mSubscription != null) {
            String thatChannelId =
                mSubscription.path("idSetSource").path("channel")
                    .path("stationId").getTextValue();
            i = 0;
            for (JsonNode offer : offers) {
              if (thatChannelId.equals(offer.path("example").path("channel")
                  .path("stationId").getTextValue())) {
                // Set spinner so it will save properly, then hide.
                Spinner s = ((Spinner) findViewById(R.id.channel));
                s.setSelection(i);
                s.setVisibility(View.GONE);
                // Set text view to display immutable (?) channel.
                TextView tv = ((TextView) findViewById(R.id.channel_text));
                tv.setText(mChannelNames[i]);
                tv.setVisibility(View.VISIBLE);
                break;
              }
              i++;
            }

            setSpinner(R.id.record_which, mWhichValues,
                mSubscription.path("showStatus").getTextValue());
            setSpinner(R.id.record_max, mMaxValues,
                mSubscription.path("maxRecordings").getIntValue());
            setSpinner(R.id.until, mUntilValues,
                mSubscription.path("keepBehavior").getTextValue());
            setSpinner(R.id.start, mStartStopValues,
                mSubscription.path("startTimePadding").getIntValue());
            setSpinner(R.id.stop, mStartStopValues,
                mSubscription.path("endTimePadding").getIntValue());
          }
        }
      };
  private String mCollectionId;
  private int mMax;
  private int mPriority = 0;
  private JsonNode mSubscription = null;
  private String mWhich;

  public void doSubscribe(View v) {
    getValues();
    doSubscribe(false);
  }

  public void doSubscribeAll(View v) {
    // Conflict dialog. Either boost priority or, if we already did that, also
    // ignore conflicts.
    if (mPriority == 0) {
      doSubscribe(true);
    } else {
      mPriority++;
      doSubscribe(false);
    }
  }

  public void doSubscribeAsIs(View v) {
    // Conflict dialog, do a ignore-conflicts subscribe.
    doSubscribe(true);
  }

  private HashMap<String, String> conflictListItem(JsonNode conflict,
      String prefix, boolean fullDate, String titleField) {
    HashMap<String, String> listItem = new HashMap<String, String>();
    listItem
        .put(prefix + "show_name", conflict.path(titleField).getTextValue());
    listItem.put(prefix + "channel",
        conflict.path("channel").path("channelNumber").getTextValue() + " "
            + conflict.path("channel").path("callSign").getTextValue());

    Date startTime =
        Utils.parseDateTimeStr(conflict.path("startTime").getTextValue());
    SimpleDateFormat dateFormatter1 =
        new SimpleDateFormat(fullDate ? "MMM dd  h:mm - " : "h:mm - ");
    dateFormatter1.setTimeZone(TimeZone.getDefault());
    String showTime = dateFormatter1.format(startTime);
    Calendar endTime = Calendar.getInstance();
    endTime.setTime(startTime);
    endTime.add(Calendar.SECOND, conflict.path("duration").getIntValue());
    SimpleDateFormat dateFormatter2 = new SimpleDateFormat("h:mm a");
    dateFormatter2.setTimeZone(TimeZone.getDefault());
    showTime += dateFormatter2.format(endTime.getTime());
    listItem.put(prefix + "show_time", showTime);

    return listItem;
  }

  private void doSubscribe(Boolean ignoreConflicts) {
    Subscribe request = new Subscribe();

    request.setCollection(mCollectionId, mChannel, mMax, mWhich);
    request.setPriority(mPriority);
    request.setIgnoreConflicts(ignoreConflicts);
    subscribeRequestCommon(request);

    // TODO: Use dialog for progress (to prevent double-button-presses).
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

    if (mPriority == 1) {
      findViewById(R.id.button2).setVisibility(View.GONE);
    }

    // "Will Record" list.
    ArrayList<HashMap<String, String>> willRecord =
        new ArrayList<HashMap<String, String>>();
    for (JsonNode conflict : conflicts.path("willGet")) {
      HashMap<String, String> listItem =
          conflictListItem(conflict, "", true, "subtitle");
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
          conflictListItem(conflict.path("losingOffer").path(0), "", true,
              "subtitle");
      listItem.putAll(conflictListItem(conflict.path("winningOffer").path(0),
          "overlap_", false, "title"));
      listItem.put("overlap_show_name",
          "Overlaps with: " + listItem.get("overlap_show_name"));
      willNotRecord.add(listItem);
    }
    // TODO: This title/subtitle policy is not quite right.
    // Sometimes, a third show is winning over the loser, because this
    // one bumped the other two down.
    for (JsonNode conflict : conflicts.path("willCancel")) {
      HashMap<String, String> listItem =
          conflictListItem(conflict.path("losingOffer").path(0), "", true,
              "title");
      listItem.putAll(conflictListItem(conflict.path("winningOffer").path(0),
          "overlap_", false, "subtitle"));
      listItem.put("overlap_show_name",
          "Overlaps with: " + listItem.get("overlap_show_name"));
      willNotRecord.add(listItem);
    }
    LinearListView willNotLv = (LinearListView) findViewById(R.id.wont_record);
    willNotLv.setAdapter(new SimpleAdapter(this, willNotRecord,
        R.layout.item_sub_wont, new String[] { "channel", "overlap_show_name",
            "overlap_show_time", "show_name", "show_time" }, new int[] {
            R.id.channel, R.id.overlap_show_name, R.id.overlap_show_time,
            R.id.show_name, R.id.show_time }));
  }

  private void setSpinner(int spinnerId, Object[] values, Object value) {
    int i = Arrays.asList(values).indexOf(value);
    if (i != -1) {
      ((Spinner) findViewById(spinnerId)).setSelection(i);
    }
  }

  @Override
  protected void getValues() {
    super.getValues();
    int pos;
    pos = ((Spinner) findViewById(R.id.channel)).getSelectedItemPosition();
    try {
      mChannel = mChannelNodes.get(pos);
    } catch (IndexOutOfBoundsException e) {
      Utils.log(Utils.join(" / ", mChannelNames));
      Utils.logError("Couldn't get channel", e);
      Toast.makeText(this, "Oops, something weird happened with the channels.",
          Toast.LENGTH_SHORT).show();
      startActivity(new Intent(this, ProblemReport.class));
      finish();
    }

    pos = ((Spinner) findViewById(R.id.record_max)).getSelectedItemPosition();
    mMax = mMaxValues[pos];

    pos = ((Spinner) findViewById(R.id.record_which)).getSelectedItemPosition();
    mWhich = mWhichValues[pos];
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    mCollectionId = bundle.getString("collectionId");

    OfferSearch request = new OfferSearch();
    request.setChannelsForCollection(mCollectionId);
    MindRpc.addRequest(request, mChannelsListener);

    String subscriptionJson = bundle.getString("subscriptionJson");
    if (subscriptionJson != null) {
      mSubscription = Utils.parseJson(subscriptionJson);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:SubscribeCollection");
    MindRpc.init(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:SubscribeCollection");
  }
}
