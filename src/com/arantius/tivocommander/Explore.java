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

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.request.RecordingUpdate;
import com.arantius.tivocommander.rpc.request.SubscriptionSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.request.Unsubscribe;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;

public class Explore extends ExploreCommon {
  enum RecordActions {
    DONT_RECORD("Don't record"), RECORD("Record this episode"), RECORD_STOP(
        "Stop recording in progress"), SP_ADD("Add season pass"), SP_CANCEL(
        "Cancel season pass"), SP_MODIFY("Modify season pass");

    private final String mText;

    private RecordActions(String text) {
      mText = text;
    }

    @Override
    public String toString() {
      return mText;
    }
  }

  private final MindRpcResponseListener mDeleteListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          getParent().setProgressBarIndeterminateVisibility(false);
          if (!("success".equals(response.getRespType()))) {
            Utils.logError("Delete attempt failed!");
            Utils.toast(Explore.this, "Delete failed!.", Toast.LENGTH_SHORT);
            return;
          }
          // .. and tell the show list to refresh itself.
          Intent resultIntent = new Intent();
          resultIntent.putExtra("refresh", true);
          getParent().setResult(Activity.RESULT_OK, resultIntent);
          finish();
        }
      };

  private final MindRpcResponseListener mRecordingListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mRecording = response.getBody().path("recording").path(0);
          mRecordingState = mRecording.path("state").asText();
          mSubscriptionType = Utils.subscriptionTypeForRecording(mRecording);
          finishRequest();
        }
      };

  private final MindRpcResponseListener mSubscriptionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mSubscription = response.getBody().path("subscription").path(0);
          if (mSubscription.isMissingNode()) {
            mSubscription = null;
            mSubscriptionId = null;
          } else {
            mSubscriptionId = mSubscription.path("subscriptionId").asText();
            final String subType =
                mSubscription.path("idSetSource").path("type").asText();
            if ("seasonPassSource".equals(subType)) {
              mSubscriptionType = SubscriptionType.SEASON_PASS;
            } else if ("wishListSource".equals(subType)) {
              mSubscriptionType = SubscriptionType.WISHLIST;
            }
          }
          finishRequest();
        }
      };

  private final ArrayList<String> mChoices = new ArrayList<String>();
  private JsonNode mRecording = null;
  private String mRecordingState = null;
  private int mRequestCount = 0;
  private SubscriptionType mSubscriptionType = null;
  private JsonNode mSubscription = null;
  private String mSubscriptionId = null;

  public void doDelete(View v) {
    // FIXME: Fails when deleting the currently-playing show.
    getParent().setProgressBarIndeterminateVisibility(true);
    String newState = "deleted";
    if (v.getId() == R.id.explore_btn_undelete) {
      newState = "complete";
    }
    // (Un-)Delete the recording ...
    final RecordingUpdate req = new RecordingUpdate(mRecordingId, newState);
    MindRpc.addRequest(req, mDeleteListener);
  }

  public void doRecord(View v) {
    ArrayAdapter<String> choicesAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            mChoices);
    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Operation?");
    dialogBuilder.setAdapter(choicesAdapter,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int position) {
            String label = mChoices.get(position);
            if (RecordActions.DONT_RECORD.toString().equals(label)) {
              getParent().setProgressBarIndeterminateVisibility(true);
              MindRpc.addRequest(
                  new RecordingUpdate(mRecordingId, "cancelled"),
                  new MindRpcResponseListener() {
                    public void onResponse(MindRpcResponse response) {
                      getParent().setProgressBarIndeterminateVisibility(false);
                      ImageView iconSubType =
                          (ImageView) findViewById(R.id.icon_sub_type);
                      TextView textSubType =
                          (TextView) findViewById(R.id.text_sub_type);
                      iconSubType.setVisibility(View.GONE);
                      textSubType.setVisibility(View.GONE);
                    }
                  });
            } else if (RecordActions.RECORD.toString().equals(label)) {
              Intent intent =
                  new Intent(getBaseContext(), SubscribeOffer.class);
              intent.putExtra("contentId", mContentId);
              intent.putExtra("offerId", mOfferId);
              startActivity(intent);
            } else if (RecordActions.RECORD_STOP.toString().equals(label)) {
              getParent().setProgressBarIndeterminateVisibility(true);
              MindRpc.addRequest(new RecordingUpdate(mRecordingId, "complete"),
                  new MindRpcResponseListener() {
                    public void onResponse(MindRpcResponse response) {
                      getParent().setProgressBarIndeterminateVisibility(false);
                      mRecordingId = null;
                    }
                  });
            } else if (RecordActions.SP_ADD.toString().equals(label)) {
              Intent intent =
                  new Intent(getBaseContext(), SubscribeCollection.class);
              intent.putExtra("collectionId", mCollectionId);
              startActivity(intent);
              // TODO: Start for result, get subscription ID.
            } else if (RecordActions.SP_CANCEL.toString().equals(label)) {
              getParent().setProgressBarIndeterminateVisibility(true);
              MindRpc.addRequest(new Unsubscribe(mSubscriptionId),
                  new MindRpcResponseListener() {
                    public void onResponse(MindRpcResponse response) {
                      getParent().setProgressBarIndeterminateVisibility(false);
                      mSubscriptionId = null;
                    }
                  });
            } else if (RecordActions.SP_MODIFY.toString().equals(label)) {
              Intent intent =
                  new Intent(getBaseContext(), SubscribeCollection.class);
              intent.putExtra("collectionId", mCollectionId);
              intent.putExtra("subscriptionId", mSubscriptionId);
              intent.putExtra("subscriptionJson",
                  Utils.stringifyToJson(mSubscription));
              startActivity(intent);
            }
          }
        });
    AlertDialog dialog = dialogBuilder.create();
    dialog.show();
  }

  public void doUpcoming(View v) {
    Intent intent = new Intent(getBaseContext(), Upcoming.class);
    intent.putExtra("collectionId", mCollectionId);
    startActivity(intent);
  }

  public void doWatch(View v) {
    MindRpc.addRequest(new UiNavigate(mRecordingId), null);
    Intent intent = new Intent(this, NowShowing.class);
    startActivity(intent);
  }

  protected void finishRequest() {
    if (--mRequestCount != 0) {
      return;
    }

    getParent().setProgressBarIndeterminateVisibility(false);

    if (mRecordingId == null) {
      for (JsonNode recording : mContent.path("recordingForContentId")) {
        String state = recording.path("state").asText();
        if ("inProgress".equals(state) || "complete".equals(state)
            || "scheduled".equals(state)) {
          mRecordingId = recording.path("recordingId").asText();
          mRecordingState = state;
          break;
        }
      }
    }

    // Fill mChoices based on the data we now have.
    if ("scheduled".equals(mRecordingState)) {
      mChoices.add(RecordActions.DONT_RECORD.toString());
    } else if ("inProgress".equals(mRecordingState)) {
      mChoices.add(RecordActions.RECORD_STOP.toString());
    } else if (mOfferId != null) {
      mChoices.add(RecordActions.RECORD.toString());
    }
    if (mSubscriptionId != null) {
      mChoices.add(RecordActions.SP_MODIFY.toString());
      mChoices.add(RecordActions.SP_CANCEL.toString());
    } else if (mCollectionId != null
        && !"movie".equals(mContent.path("collectionType").asText())
        && !mContent.has("movieYear")) {
      mChoices.add(RecordActions.SP_ADD.toString());
    }

    setContentView(R.layout.explore);

    // Show only appropriate buttons.
    findViewById(R.id.explore_btn_watch).setVisibility(
        "complete".equals(mRecordingState)
            || "inprogress".equals(mRecordingState)
            ? View.VISIBLE : View.GONE);
    hideViewIfNull(R.id.explore_btn_upcoming, mCollectionId);
    if (mChoices.size() == 0) {
      findViewById(R.id.explore_btn_record).setVisibility(View.GONE);
    }
    // Delete / undelete buttons visible only if appropriate.
    findViewById(R.id.explore_btn_delete).setVisibility(
        "complete".equals(mRecordingState) ? View.VISIBLE : View.GONE);
    findViewById(R.id.explore_btn_undelete).setVisibility(
        "deleted".equals(mRecordingState) ? View.VISIBLE : View.GONE);

    // Display titles.
    String title = mContent.path("title").asText();
    String subtitle = mContent.path("subtitle").asText();
    ((TextView) findViewById(R.id.content_title)).setText(title);
    TextView subtitleView = ((TextView) findViewById(R.id.content_subtitle));
    if ("".equals(subtitle)) {
      subtitleView.setVisibility(View.GONE);
    } else {
      subtitleView.setText(subtitle);
    }

    // Display (only the proper) badges.
    if (mRecording == null || mRecording.path("repeat").asBoolean()) {
      findViewById(R.id.badge_new).setVisibility(View.GONE);
    }
    if (mRecording == null || !mRecording.path("hdtv").asBoolean()) {
      findViewById(R.id.badge_hd).setVisibility(View.GONE);
    }
    ImageView iconSubType = (ImageView) findViewById(R.id.icon_sub_type);
    TextView textSubType = (TextView) findViewById(R.id.text_sub_type);
    // TODO: Downloading state?
    if ("complete".equals(mRecordingState)) {
      iconSubType.setVisibility(View.GONE);
      textSubType.setVisibility(View.GONE);
    } else if ("inProgress".equals(mRecordingState)) {
      iconSubType.setImageResource(R.drawable.recording_recording);
      textSubType.setText(R.string.sub_recording);
    } else if (mSubscriptionType != null) {
      switch (mSubscriptionType) {
      case SEASON_PASS:
        iconSubType.setImageResource(R.drawable.todo_seasonpass);
        textSubType.setText(R.string.sub_season_pass);
        break;
      case SINGLE_OFFER:
        iconSubType.setImageResource(R.drawable.todo_single_offer);
        textSubType.setText(R.string.sub_single_offer);
        break;
      case WISHLIST:
        iconSubType.setImageResource(R.drawable.todo_wishlist);
        textSubType.setText(R.string.sub_wishlist);
        break;
      default:
        iconSubType.setVisibility(View.GONE);
        textSubType.setVisibility(View.GONE);
      }
    } else {
      iconSubType.setVisibility(View.GONE);
      textSubType.setVisibility(View.GONE);
    }

    // Display channel and time.
    if (mRecording != null) {
      String channelStr = "";
      JsonNode channel = mRecording.path("channel");
      if (!channel.isMissingNode()) {
        channelStr =
            String.format("%s %s, ", channel.path("channelNumber")
                .asText(),
                channel.path("callSign").asText());
      }

      // Lots of shows seem to be a few seconds short, add padding so that
      // rounding down works as expected. Magic number.
      final int minutes = (30 + mRecording.path("duration").asInt()) / 60;

      String durationStr =
          minutes >= 60 ? String.format(Locale.US, "%d hr", minutes / 60)
              : String.format(Locale.US, "%d min", minutes);
      if (isRecordingPartial()) {
        durationStr += " (partial)";
      }
      ((TextView) findViewById(R.id.content_time)).setText(channelStr
          + durationStr);
    } else {
      ((TextView) findViewById(R.id.content_time)).setVisibility(View.GONE);
    }

    // Construct and display details.
    ArrayList<String> detailParts = new ArrayList<String>();
    int season = mContent.path("seasonNumber").asInt();
    int epNum = mContent.path("episodeNum").path(0).asInt();
    if (season != 0 && epNum != 0) {
      detailParts.add(String.format("Sea %d Ep %d", season, epNum));
    }
    if (mContent.has("mpaaRating")) {
      detailParts.add(mContent.path("mpaaRating").asText()
          .toUpperCase(Locale.US));
    } else if (mContent.has("tvRating")) {
      detailParts.add("TV-"
          + mContent.path("tvRating").asText().toUpperCase(Locale.US));
    }
    detailParts.add(mContent.path("category").path(0).path("label")
        .asText());
    int year = mContent.path("originalAirYear").asInt();
    if (year != 0) {
      detailParts.add(Integer.toString(year));
    }

    // Filter empty strings.
    for (int i = detailParts.size() - 1; i >= 0; i--) {
      if ("".equals(detailParts.get(i)) || null == detailParts.get(i)) {
        detailParts.remove(i);
      }
    }
    // Then format the parts into one string.
    String detail1 = "(" + Utils.join(", ", detailParts) + ") ";
    if ("() ".equals(detail1)) {
      detail1 = "";
    }

    String detail2 = mContent.path("description").asText();
    TextView detailView = ((TextView) findViewById(R.id.content_details));
    if (detail2 == null) {
      detailView.setText(detail1);
    } else {
      Spannable details = new SpannableString(detail1 + detail2);
      details.setSpan(new ForegroundColorSpan(Color.WHITE), detail1.length(),
          details.length(), 0);
      detailView.setText(details);
    }

    // Add credits.
    ArrayList<String> credits = new ArrayList<String>();
    for (JsonNode credit : mContent.path("credit")) {
      String role = credit.path("role").asText();
      if ("actor".equals(role) || "host".equals(role)
          || "guestStar".equals(role)) {
        credits.add(credit.path("first").asText() + " "
            + credit.path("last").asText());
      }
    }
    TextView creditsView = (TextView) findViewById(R.id.content_credits);
    creditsView.setText(Utils.join(", ", credits));

    // Find and set the banner image if possible.
    ImageView imageView = (ImageView) findViewById(R.id.content_image);
    View progressView = findViewById(R.id.content_image_progress);
    String imageUrl = Utils.findImageUrl(mContent);
    new DownloadImageTask(this, imageView, progressView).execute(imageUrl);
  }

  private void hideViewIfNull(int viewId, Object condition) {
    if (condition != null)
      return;
    findViewById(viewId).setVisibility(View.GONE);
  }

  private Boolean isRecordingPartial() {
    final Date actualStart =
        Utils.parseDateTimeStr(mRecording.path("actualStartTime")
            .asText());
    final Date scheduledStart =
        Utils.parseDateTimeStr(mRecording.path("scheduledStartTime")
            .asText());
    final Date actualEnd =
        Utils.parseDateTimeStr(mRecording.path("actualEndTime").asText());
    final Date scheduledEnd =
        Utils.parseDateTimeStr(mRecording.path("scheduledEndTime")
            .asText());
    return actualStart.getTime() - scheduledStart.getTime() >= 30000
        || scheduledEnd.getTime() - actualEnd.getTime() >= 30000;
  }

  @Override
  protected void onContent() {
    finishRequest();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Utils.log(String.format("Explore: "
        + "contentId:%s collectionId:%s offerId:%s recordingId:%s", mContentId,
        mCollectionId, mOfferId, mRecordingId));

    // The one from ExploreCommon.
    mRequestCount = 1;

    if (mCollectionId != null) {
      mRequestCount++;
      MindRpc.addRequest(new SubscriptionSearch(mCollectionId),
          mSubscriptionListener);
    }

    if (mRecordingId != null) {
      mRequestCount++;
      MindRpc.addRequest(new RecordingSearch(mRecordingId), mRecordingListener);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Explore");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Explore");
    if (MindRpc.init(this, null)) {
      return;
    }
  }
}
