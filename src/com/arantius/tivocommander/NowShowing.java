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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.BodyConfigSearch;
import com.arantius.tivocommander.rpc.request.CancelRpc;
import com.arantius.tivocommander.rpc.request.OfferSearch;
import com.arantius.tivocommander.rpc.request.VideoPlaybackInfoEventRegister;
import com.arantius.tivocommander.rpc.request.WhatsOnSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class NowShowing extends Activity {
  private enum ContentType {
    LIVE, RECORDING;
  }

  private final MindRpcResponseListener mBodyConfigCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          final JsonNode bodyConfig =
              response.getBody().path("bodyConfig").path(0);
          MindRpc.saveBodyId(bodyConfig.path("bodyId").getTextValue());

          int gmtOffsetSeconds = bodyConfig.path("secondsFromGmt").asInt();
          mGmtOffsetMillis = gmtOffsetSeconds * 1000;

          rpcComplete();
        }
      };
  private final MindRpcResponseListener mOfferContentCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode offer = response.getBody().path("offer").path(0);
//          Utils.log(Utils.stringifyToPrettyJson(offer));

          // Set title and subtitle.
          ((TextView) findViewById(R.id.content_title)).setText(offer.path(
              "title").asText());
          String subtitle = offer.path("subtitle").asText();
          TextView subtitleView = (TextView) findViewById(R.id.content_subtitle);
          if (null == subtitle || "".equals(subtitle)) {
            subtitleView.setVisibility(View.GONE);
          } else {
            subtitleView.setVisibility(View.VISIBLE);
            subtitleView.setText(subtitle);
          }

          Date now = new Date();
          Utils.log("current time              " + mDateFormat.format(now));
          // Find the node that is the source of time information.
          JsonNode timeSource = offer;
          Utils.log(String.format(Locale.US,
              "Time from offer:          %s; %d",
              offer.path("startTime").asText(),
              offer.path("duration").asLong()));
          if (mContentType == ContentType.RECORDING) {
            if (offer.has("recordingForContentId")) {
              timeSource = offer.path("recordingForContentId").path(0);
              Utils.log(String.format(Locale.US,
                  "Time from recording:      %s; %d",
                  timeSource.path("startTime").asText(),
                  timeSource.path("duration").asLong()));
            } else {
              Utils.log("no recording!");
            }
          } else {
            Utils.log("live content type");
          }

          // Calculate absolute start and end times.
          final Integer durationSec = timeSource.path("duration").asInt();
          final String startTimeStr = timeSource.path("startTime").asText();
          try {
            Date offerBeginDate = mDateFormat.parse(startTimeStr);
            mMillisOfferBegin = offerBeginDate.getTime();
          } catch (ParseException e) {
            Utils.logError(
                "Failed to parse start time " + startTimeStr, e);
            mMillisOfferBegin = 0L;
          }
          mMillisOfferEnd = mMillisOfferBegin + (durationSec * 1000);

          Utils.log(String.format(Locale.US,
              "offer begin %d %s",
              mMillisOfferBegin, mDateFormat.format(new Date(mMillisOfferBegin))
              ));
          Utils.log(String.format(Locale.US,
              "offer end   %d %s",
              mMillisOfferEnd, mDateFormat.format(new Date(mMillisOfferEnd))
              ));

          setSeekbarLabels();
          rpcComplete();
        };
      };
  private final MindRpcResponseListener mPlaybackInfoCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mHavePlaybackInfo = true;
          JsonNode playbackInfo = response.getBody();
//          Utils.log(Utils.stringifyToPrettyJson(playbackInfo));

          mMillisPosition = playbackInfo.path("position").asLong();
          mMillisRecordingBegin = playbackInfo.path("begin").asLong();
          mMillisRecordingEnd = playbackInfo.path("end").asLong();
          mMillisVirtualPosition = playbackInfo.path("virtualPosition").asLong();

          Utils.log(String.format(Locale.US,
              "playback info [%d %d %d]",
              mMillisRecordingBegin, mMillisPosition, mMillisRecordingEnd
              ));
          Utils.log(String.format(Locale.US,
              "virt. pos.  %d %s",
              mMillisVirtualPosition,
              mDateFormat.format(new Date(mMillisVirtualPosition))
              ));

          if (mMillisRecordingBegin != 0) {
            // Normal for (e.g. live) recording that started late?
            Utils.log("PlaybackInfo with non-0 begin:");
            Utils.log(Utils.stringifyToPrettyJson(playbackInfo));
          }

          setSeekbarPositions();
          rpcComplete();
        };
      };
  private final MindRpcResponseListener mWhatsOnCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode whatsOn = response.getBody().path("whatsOn").path(0);
          String offerId = whatsOn.path("offerId").asText();

          // If the offer hasn't changed, this was an unnecessary callback.
          if (mOfferId != null) {
            if (mOfferId.equals(offerId)) {
              return;
            } else {
              // If we already have some other offer ID, we need to start over.
              initInstanceVars();
            }
          }
          mOfferId = offerId;

          // Otherwise, we need to load details.  Turn on loading indicator.
          ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
              .setDisplayedChild(1);

          String playbackType = whatsOn.path("playbackType").asText();
          if ("recording".equals(playbackType)) {
            // A recording is being played.
            mContentType = ContentType.RECORDING;
          } else if ("liveCache".equals(playbackType)) {
            // A live show is being played.
            mContentType = ContentType.LIVE;
          }

          // TODO: The offer is not always available!  For too-old recordings?
          // Consider requesting the recording details instead?
          OfferSearch request = new OfferSearch("offerId", offerId);
          MindRpc.addRequest(request, mOfferContentCallback);
        };
      };

  final private SimpleDateFormat mDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", Locale.US);
  final private SimpleDateFormat mDisplayTimeFormat = new SimpleDateFormat(
      "hh:mm", Locale.US);
  private Integer mGmtOffsetMillis = null;

  private ContentType mContentType = null;
  private boolean mHavePlaybackInfo = false;
  private SeekBar mSeekBar = null;
  private Long mMillisOfferBegin = null;
  private Long mMillisOfferEnd = null;
  private Long mMillisPosition = null;
  private Long mMillisRecordingBegin = null;
  private Long mMillisRecordingEnd = null;
  private Long mMillisVirtualPosition = null;
  private String mOfferId = null;
  private boolean mRpcComplete = false;
  private Integer mRpcIdPlaybackInfo = null;
  private Integer mRpcIdWhatsOn = null;

  public final void cancelOutstandingRpcs(View v) {
    if (mRpcIdPlaybackInfo != null) {
      MindRpc.addRequest(new CancelRpc(mRpcIdPlaybackInfo), null);
    }
    mRpcIdPlaybackInfo = null;

    if (mRpcIdWhatsOn != null) {
      MindRpc.addRequest(new CancelRpc(mRpcIdWhatsOn), null);
    }
    mRpcIdWhatsOn = null;
  }

  public void onClickButton(View target) {
    Intent intent = null;
    switch (target.getId()) {
    case R.id.target_remote:
      intent = new Intent(getBaseContext(), Remote.class);
      break;
    case R.id.target_myshows:
      intent = new Intent(getBaseContext(), MyShows.class);
      break;
    case R.id.target_search:
      intent = new Intent(getBaseContext(), Search.class);
      break;
    case R.id.target_settings:
      intent = new Intent(getBaseContext(), Discover.class);
      break;
    case R.id.target_help:
      intent = new Intent(getBaseContext(), Help.class);
      break;
    case R.id.target_about:
      intent = new Intent(getBaseContext(), About.class);
      break;
    }

    if (intent != null) {
      startActivity(intent);
    }
  }

  public void onClickRemote(View v) {
    MindRpc.addRequest(Remote.viewIdToEvent(v.getId()), null);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:NowPlaying");
    cancelOutstandingRpcs(null);
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:NowPlaying");
    setContentView(R.layout.now_showing);
    setTitle("Now Showing");

    mSeekBar = (SeekBar) findViewById(R.id.now_showing_seek);
    TimeZone gmtTz = TimeZone.getTimeZone("GMT");
    mDateFormat.setTimeZone(gmtTz);

    if (!MindRpc.getBodyIsAuthed()) {
      Intent intent = new Intent(getBaseContext(), Connect.class);
      startActivity(intent);
      return;
    }

    // Turn on loading indicator.
    ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
        .setDisplayedChild(1);
    initInstanceVars();

    MindRpc.addRequest(new BodyConfigSearch(), mBodyConfigCallback);

    WhatsOnSearch whatsOnRequest = new WhatsOnSearch();
    mRpcIdWhatsOn = whatsOnRequest.getRpcId();
    MindRpc.addRequest(whatsOnRequest, mWhatsOnCallback);

    VideoPlaybackInfoEventRegister playbackInfoRequest =
        new VideoPlaybackInfoEventRegister(1001);
    mRpcIdPlaybackInfo = playbackInfoRequest.getRpcId();
    MindRpc.addRequest(playbackInfoRequest, mPlaybackInfoCallback);
  }

  /** Initialize instance variables. */
  private void initInstanceVars() {
    mContentType = null;
    mHavePlaybackInfo = false;
    mMillisOfferBegin = null;
    mMillisOfferEnd = null;
    mMillisPosition = null;
    mMillisRecordingBegin = null;
    mMillisRecordingEnd = null;
    mMillisVirtualPosition = null;
    mRpcComplete = false;
  }

  /** After any given RPC completes, check state (given all) and continue. */
  private void rpcComplete() {
    if (mRpcComplete) return;

    if (mGmtOffsetMillis == null) return;
    if (mMillisOfferBegin == null) return;
    if (mMillisOfferEnd == null) return;
    if (mMillisPosition == null) return;
    if (mMillisRecordingBegin == null) return;
    if (mMillisRecordingEnd == null) return;
    if (mMillisVirtualPosition == null) return;
    if (!mHavePlaybackInfo) return;

    mRpcComplete = true;

    // Ensure these have been run, regardless of previous ordering.
    setSeekbarLabels();
    setSeekbarPositions();
    // Then make them visible.
    ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
        .setDisplayedChild(0);
  }

  /** Set the start/end time labels of the seek bar. */
  private void setSeekbarLabels() {
    final TextView startLabel =
        (TextView) findViewById(R.id.content_start_time);
    final TextView endLabel =
        (TextView) findViewById(R.id.content_end_time);

    if (mContentType == ContentType.LIVE) {
      Date beginDate = new Date(mMillisOfferBegin + mGmtOffsetMillis);
      Date endDate = new Date(mMillisOfferEnd + mGmtOffsetMillis);

      // Show absolute start and end time.
      startLabel.setVisibility(View.VISIBLE);
      startLabel.setText(mDisplayTimeFormat.format(beginDate));
      endLabel.setText(mDisplayTimeFormat.format(endDate));
    } else if (mContentType == ContentType.RECORDING) {
      Date durationDate = new Date(mMillisOfferEnd - mMillisOfferBegin);
      // Just show duration as end time.
      startLabel.setVisibility(View.GONE);
      endLabel.setText(mDisplayTimeFormat.format(durationDate));
    }
  }

  /** Set the current, and available start/end positions, of the seek bar. */
  private void setSeekbarPositions() {
    if (!mRpcComplete) return;

    Long millisVirtualPosition = mMillisVirtualPosition;
//    if (mContentType == ContentType.LIVE) {
//      // This is surprising .. the virtualPosition is in GMT for timeshifted
//      // recordings, but for live playback is in local time?!  SUBTRACT
//      // offset, to get back to GMT.
//      millisVirtualPosition -= mGmtOffsetMillis;
//    }

    int max = (int) (mMillisOfferEnd - mMillisOfferBegin);
    int progress = (int) (millisVirtualPosition - mMillisOfferBegin);
    int activeMin = (int) (progress - mMillisPosition);
    int activeMax = (int) (activeMin + mMillisRecordingEnd);

    // Live shows sometime have a bit of the previous show recorded also.
    // The TiVo displays a seek bar with the whole recorded range; for now,
    // we just truncate for simplicity.
    if (activeMin < 0) {
      activeMax += activeMin;
      activeMin = 0;
    }

//    Utils.log(String.format(
//        Locale.US,
//        "offer [%,d %,d %,d] %,d %,d",
//        mMillisOfferBegin, millisVirtualPosition, mMillisOfferEnd,
//        mMillisOfferEnd - mMillisOfferBegin,
//        mMillisOfferEnd - millisVirtualPosition
//        ));
//    Utils.log(String.format(
//        Locale.US,
//        "record [%,d %,d %,d] %,d offset: %,d",
//        mMillisRecordingBegin, mMillisPosition, mMillisRecordingEnd,
//        mMillisRecordingEnd - mMillisRecordingBegin, mGmtOffsetMillis
//        ));
//    Utils.log(String.format(
//        Locale.US,
//        "calc %,d %,d %,d %,d",
//        activeMin, progress, activeMax, max
//        ));

    mSeekBar.setMax(max);
    mSeekBar.setProgress(progress);
  }
}
