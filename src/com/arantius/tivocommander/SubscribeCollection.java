package com.arantius.tivocommander;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.OfferSearch;
import com.arantius.tivocommander.rpc.request.Subscribe;
import com.arantius.tivocommander.rpc.request.SubscriptionSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.arantius.tivocommander.views.LinearListView;
import com.fasterxml.jackson.databind.JsonNode;

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
  private JsonNode mOffers;
  private int mRequestCount = 0;
  private String mCollectionId;
  private int mMax;
  private int mPriority = 0;
  private JsonNode mSubscription = null;
  private String mWhich;

  private final MindRpcResponseListener mChannelsListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mOffers = response.getBody().path("offerGroup");
          mChannelNodes.clear();
          mChannelNames = new String[mOffers.size()];

          int i = 0;
          for (JsonNode offer : mOffers) {
            JsonNode channel = offer.path("example").path("channel");
            mChannelNodes.add(channel);
            mChannelNames[i++] =
                channel.path("channelNumber").asText() + " "
                    + channel.path("callSign").asText();
          }
          if (i == 0) {
            Utils.toast(SubscribeCollection.this,
                "Sorry: Couldn't find any channels to record that on.",
                Toast.LENGTH_SHORT);
            finish();
            return;
          }

          finishRequest();
        }
      };

  private final MindRpcResponseListener mSubscriptionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mSubscription = response.getBody().path("subscription").path(0);
          finishRequest();
        }
      };

  private HashMap<String, String> conflictListItem(JsonNode conflict,
      String prefix, boolean fullDate, String titleField) {
    HashMap<String, String> listItem = new HashMap<String, String>();
    String title = conflict.path(titleField).asText();
    if ("".equals(title) && "subtitle".equals(titleField)) {
      title = conflict.path("title").asText();
    }
    listItem
        .put(prefix + "show_name", title);
    listItem.put(prefix + "channel",
        conflict.path("channel").path("channelNumber").asText() + " "
            + conflict.path("channel").path("callSign").asText());

    Date startTime =
        Utils.parseDateTimeStr(conflict.path("startTime").asText());
    SimpleDateFormat dateFormatter1 =
        new SimpleDateFormat(fullDate ? "MMM dd h:mm - " : "h:mm - ",
            Locale.US);
    dateFormatter1.setTimeZone(TimeZone.getDefault());
    String showTime = dateFormatter1.format(startTime);
    Calendar endTime = Calendar.getInstance();
    endTime.setTime(startTime);
    endTime.add(Calendar.SECOND, conflict.path("duration").asInt());
    SimpleDateFormat dateFormatter2 = new SimpleDateFormat("h:mm a", Locale.US);
    dateFormatter2.setTimeZone(TimeZone.getDefault());
    showTime += dateFormatter2.format(endTime.getTime());
    listItem.put(prefix + "show_time", showTime);

    return listItem;
  }

  private void doSubscribe(Boolean ignoreConflicts) {
    final Subscribe request = new Subscribe();

    final String subscriptionId = (mSubscription == null)
        ? null : mSubscription.path("subscriptionId").textValue();

    request
        .setCollection(mCollectionId, mChannel, mMax, mWhich, subscriptionId);
    if (mSubscription != null && subscriptionId != null) {
      request.setIgnoreConflicts(true);
      request.setPriority(mSubscription.path("priority").asInt());
    } else {
      request.setIgnoreConflicts(ignoreConflicts);
      request.setPriority(mPriority);
    }
    subscribeRequestCommon(request);

    final ProgressDialog d = new ProgressDialog(this);
    d.setIndeterminate(true);
    d.setTitle("Subscribing ...");
    d.setMessage("Saving season pass.");
    d.setCancelable(false);
    d.show();

    MindRpc.addRequest(request, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        if ("error".equals(response.getBody().path("type").asText())) {
          final String msg = "Error making subscription: "
              + response.getBody().path("text").asText();
          Utils.toast(SubscribeCollection.this, msg, Toast.LENGTH_SHORT);
          d.dismiss();
          finish();
        } else if (response.getBody().has("conflicts")) {
          d.dismiss();
          handleConflicts(response.getBody().path("conflicts"));
        } else if (response.getBody().has("subscription")) {
          d.dismiss();
          finish();
        } else {
          Utils.log("What kind of subscribe response is this??");
          Utils.log(Utils.stringifyToPrettyJson(response.getBody()));
        }
      }
    });
  }

  public void doSubscribe(View v) {
    getValues();
    doSubscribe(false);
  }

  public void doSubscribeAll(View v) {
    // Conflict dialog. Either boost priority or, if we already did that, also
    // ignore conflicts.
    if (mPriority == 0) {
      mPriority++;
      doSubscribe(false);
    } else {
      doSubscribe(true);
    }
  }

  public void doSubscribeAsIs(View v) {
    // Conflict dialog, do a ignore-conflicts subscribe.
    doSubscribe(true);
  }

  protected void finishRequest() {
    if (--mRequestCount > 0) {
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
      final String thatChannelId =
          mSubscription.path("idSetSource").path("channel")
              .path("stationId").asText();
      int i = 0;
      for (JsonNode offer : mOffers) {
        final String thisChannelId =
            offer.path("example").path("channel")
                .path("stationId").asText();
        if (thatChannelId.equals(thisChannelId)) {
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
          mSubscription.path("showStatus").asText());
      setSpinner(R.id.record_max, mMaxValues,
          mSubscription.path("maxRecordings").asInt());
      setSpinner(R.id.until, mUntilValues,
          mSubscription.path("keepBehavior").asText());
      setSpinner(R.id.start, mStartStopValues,
          mSubscription.path("startTimePadding").asInt());
      setSpinner(R.id.stop, mStartStopValues,
          mSubscription.path("endTimePadding").asInt());
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
      Utils.toast(this, "Oops, something weird happened with the channels.",
          Toast.LENGTH_SHORT);
      finish();
    }

    pos = ((Spinner) findViewById(R.id.record_max)).getSelectedItemPosition();
    mMax = mMaxValues[pos];

    pos = ((Spinner) findViewById(R.id.record_which)).getSelectedItemPosition();
    mWhich = mWhichValues[pos];
  }

  private void handleConflicts(JsonNode conflicts) {
    Utils.showProgress(this, false);
    setContentView(R.layout.subscribe_conflicts);

    if (mPriority == 1) {
      findViewById(R.id.button_get_all).setVisibility(View.GONE);
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

    // TODO: Will clip list.
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Bundle bundle = getIntent().getExtras();
    if (MindRpc.init(this, bundle)) {
      return;
    }

    setContentView(R.layout.progress);

    mCollectionId = bundle.getString("collectionId");

    OfferSearch request = new OfferSearch();
    request.setChannelsForCollection(mCollectionId);
    mRequestCount++;
    MindRpc.addRequest(request, mChannelsListener);

    String subscriptionJson = bundle.getString("subscriptionJson");
    if (subscriptionJson != null) {
      mSubscription = Utils.parseJson(subscriptionJson);
      mRequestCount++;
      finishRequest();
    } else {
      mRequestCount++;
      MindRpc.addRequest(new SubscriptionSearch(mCollectionId),
          mSubscriptionListener);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:SubscribeCollection");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:SubscribeCollection");
    MindRpc.init(this, getIntent().getExtras());
  }

  private void setSpinner(int spinnerId, Object[] values, Object value) {
    int i = Arrays.asList(values).indexOf(value);
    if (i != -1) {
      ((Spinner) findViewById(spinnerId)).setSelection(i);
    }
  }
}
