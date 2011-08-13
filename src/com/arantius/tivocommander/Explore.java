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

import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

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
import com.arantius.tivocommander.rpc.request.RecordingUpdate;
import com.arantius.tivocommander.rpc.request.SubscriptionSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Explore extends ExploreCommon {
  enum RecordActions {
    RECORD("Record this episode"), SP_ADD("Add season pass"), SP_CANCEL(
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

  private String mRecordingId = null;
  private int mRequestCount = 0;
  private String mSubscriptionId = null;
  private final MindRpcResponseListener mSubscriptionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mSubscriptionId =
              response.getBody().path("subscription").path(0)
                  .path("subscriptionId").getTextValue();
          finishRequest();
        }
      };

  public void doDelete(View v) {
    getParent().setProgressBarIndeterminateVisibility(true);
    // Delete the recording ...
    MindRpc.addRequest(new RecordingUpdate(mRecordingId, "deleted"),
        new MindRpcResponseListener() {
          public void onResponse(MindRpcResponse response) {
            getParent().setProgressBarIndeterminateVisibility(false);
            if (!("success".equals(response.getRespType()))) {
              Utils.logError("Delete attempt failed!");
              Toast.makeText(getBaseContext(), "Delete failed!.",
                  Toast.LENGTH_SHORT).show();
              return;
            }
            // .. and tell the show list to refresh itself.
            Intent resultIntent = new Intent();
            resultIntent.putExtra("refresh", true);
            getParent().setResult(Activity.RESULT_OK, resultIntent);
            finish();
          }
        });
  }

  public void doRecord(View v) {
    final ArrayList<String> choices = new ArrayList<String>();

    if (mOfferId != null) {
      choices.add(RecordActions.RECORD.toString());
    }

    if (mSubscriptionId != null) {
      choices.add(RecordActions.SP_MODIFY.toString());
      choices.add(RecordActions.SP_CANCEL.toString());
    } else if (mCollectionId != null) {
      choices.add(RecordActions.SP_ADD.toString());
    }

    // TODO: Stop active recording.
    // TODO: Cancel future recording.

    ArrayAdapter<String> choicesAdapter =
        new ArrayAdapter<String>(this, android.R.layout.select_dialog_item,
            choices);
    Builder dialogBuilder = new AlertDialog.Builder(this);
    dialogBuilder.setTitle("Operation?");
    dialogBuilder.setAdapter(choicesAdapter,
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int position) {
            String label = choices.get(position);
            if (RecordActions.RECORD.toString().equals(label)) {
              Intent intent =
                  new Intent(getBaseContext(), SubscribeOffer.class);
              intent.putExtra("contentId", mContentId);
              intent.putExtra("offerId", mOfferId);
              startActivity(intent);
            } else if (RecordActions.SP_ADD.toString().equals(label)) {
              Intent intent =
                  new Intent(getBaseContext(), SubscribeCollection.class);
              intent.putExtra("collectionId", mCollectionId);
              startActivity(intent);
            } else if (RecordActions.SP_CANCEL.toString().equals(label)) {
            } else if (RecordActions.SP_MODIFY.toString().equals(label)) {
            }
          }
        });
    AlertDialog dialog = dialogBuilder.create();
    dialog.show();
  }

  // TODO: doSeasonPass()
  // TODO: doStopRecording()

  public void doUpcoming(View v) {
    Intent intent = new Intent(getBaseContext(), Upcoming.class);
    intent.putExtra("collectionId", mCollectionId);
    startActivity(intent);
  }

  public void doWatch(View v) {
    MindRpc.addRequest(new UiNavigate(mRecordingId), null);
  }

  private void hideViewIfNull(int viewId, Object condition) {
    if (condition != null)
      return;
    findViewById(viewId).setVisibility(View.GONE);
  }

  protected void finishRequest() {
    if (--mRequestCount != 0) {
      return;
    }

    getParent().setProgressBarIndeterminateVisibility(false);

    for (JsonNode recording : mContent.path("recordingForContentId")) {
      if ("inProgress".equals(recording.path("state").getTextValue())) {
        mRecordingId = recording.path("recordingId").getTextValue();
        break;
      }
    }

    setContentView(R.layout.explore);

    // Show only appropriate buttons.
    hideViewIfNull(R.id.explore_btn_watch, mRecordingId);
    hideViewIfNull(R.id.explore_btn_delete, mRecordingId);
    hideViewIfNull(R.id.explore_btn_upcoming, mCollectionId);

    // Display titles.
    String title = mContent.path("title").getTextValue();
    String subtitle = mContent.path("subtitle").getTextValue();
    ((TextView) findViewById(R.id.content_title)).setText(title);
    TextView subtitleView = ((TextView) findViewById(R.id.content_subtitle));
    if (subtitle == null) {
      setTitle(title);
      subtitleView.setVisibility(View.GONE);
    } else {
      setTitle(title + " - " + subtitle);
      subtitleView.setText(subtitle);
    }

    // Construct and display details.
    ArrayList<String> detailParts = new ArrayList<String>();
    int season = mContent.path("seasonNumber").getIntValue();
    int epNum = mContent.path("episodeNum").path(0).getIntValue();
    if (season != 0 && epNum != 0) {
      detailParts.add(String.format("Sea %d Ep %d", season, epNum));
    }
    if (mContent.has("mpaaRating")) {
      detailParts.add(mContent.path("mpaaRating").getTextValue().toUpperCase());
    } else if (mContent.has("tvRating")) {
      detailParts.add("TV-"
          + mContent.path("tvRating").getTextValue().toUpperCase());
    }
    detailParts.add(mContent.path("category").path(0).path("label")
        .getTextValue());
    int year = mContent.path("originalAirYear").getIntValue();
    if (year != 0) {
      detailParts.add(Integer.toString(year));
    }
    String detail1 = "(" + Utils.join(", ", detailParts) + ") ";
    if ("() ".equals(detail1)) {
      detail1 = "";
    }
    String detail2 = mContent.path("description").getTextValue();
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
      String role = credit.path("role").getTextValue();
      if ("actor".equals(role) || "host".equals(role)
          || "guestStar".equals(role)) {
        credits.add(credit.path("first").getTextValue() + " "
            + credit.path("last").getTextValue());
      }
    }
    TextView creditsView = (TextView) findViewById(R.id.content_credits);
    creditsView.setText(Utils.join(", ", credits));

    // Find and set the banner image if possible.
    ImageView imageView = (ImageView) findViewById(R.id.content_image);
    View progressView = findViewById(R.id.content_image_progress);
    String imageUrl = Utils.findImageUrl(mContent);
    new DownloadImageTask(this, imageView, progressView).execute(imageUrl);

    // TODO: Show date recorded (?).
  }

  @Override
  protected void onContent() {
    finishRequest();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    Utils.log(String.format(
        "Exploring contentId %s, collectionId %s, offerId %s", mContentId,
        mCollectionId, mOfferId));

    // The one from ExploreCommon, plus this one.
    mRequestCount = 2;

    MindRpc.addRequest(new SubscriptionSearch(mCollectionId),
        mSubscriptionListener);
  }
}
