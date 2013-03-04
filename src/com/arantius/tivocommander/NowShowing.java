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
import java.util.Calendar;
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
          TimeZone tz = TimeZone.getTimeZone("GMT");
          tz.setRawOffset(gmtOffsetSeconds * 1000);
          mLocalZone = tz;
          rpcComplete();
        }
      };
  private final MindRpcResponseListener mOfferContentCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          // Set title and subtitle.
          JsonNode offer = response.getBody().path("offer").path(0);
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

          // Find the node that is the source of time information.
          JsonNode timeSource = offer;
          // TODO: Does this bit work right?  The offer is right where this is
          // wrong, rarely.
          if (offer.has("recordingForContentId")) {
            timeSource = offer.path("recordingForContentId").path(0);
          }

          // Calculate absolute start and end times.
          final Integer durationSec = timeSource.path("duration").asInt();
          final String startTimeStr = timeSource.path("startTime").asText();
          try {
            mTimeStart = Calendar.getInstance(mGmtZone);
            mTimeStart.setTime(mDateFormat.parse(startTimeStr));
          } catch (ParseException e) {
            Utils.logError(
                "Failed to parse start time " + startTimeStr, e);
            mTimeStart.setTime(new Date(0));
          }
          // TODO: scheduled, not currently recorded, duration.
          mTimeEnd = (Calendar) mTimeStart.clone();
          mTimeEnd.add(Calendar.SECOND, durationSec);

          rpcComplete();
        };
      };
  private final MindRpcResponseListener mPlaybackInfoCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mHavePlaybackInfo = true;
          JsonNode playbackInfo = response.getBody();

          Long virtualPositionMillis =
              playbackInfo.path("virtualPosition").asLong();
          Long beginMillis = playbackInfo.path("begin").asLong();
          Long endMillis = playbackInfo.path("end").asLong();
          Long positionMillis = playbackInfo.path("position").asLong();

          if (beginMillis != 0) {
            Utils.log("PlaybackInfo with non-0 begin:");
            Utils.log(Utils.stringifyToPrettyJson(playbackInfo));
          }

          mTimeVirtualPosition = Calendar.getInstance(mLocalZone);
          mTimeVirtualPosition.setTimeInMillis(virtualPositionMillis);
          mTimeAvailableStart = Calendar.getInstance(mGmtZone);
          mTimeAvailableStart.setTimeInMillis(
              virtualPositionMillis - positionMillis);
          mTimeAvailableEnd = Calendar.getInstance(mGmtZone);
          mTimeAvailableEnd.setTimeInMillis(
              mTimeAvailableStart.getTimeInMillis() + endMillis);

          rpcComplete();
          // TODO: Save as instance vars rather than pass; so it can be retried.
          setSeekbarPositions();
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

          OfferSearch request = new OfferSearch("offerId", offerId);
          MindRpc.addRequest(request, mOfferContentCallback);
        };
      };

  final private SimpleDateFormat mDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", Locale.US);
  final private SimpleDateFormat mDisplayTimeFormat = new SimpleDateFormat(
      "hh:mm", Locale.US);
  final private TimeZone mGmtZone = TimeZone.getTimeZone("GMT");

  private ContentType mContentType = null;
  private boolean mHavePlaybackInfo = false;
  private TimeZone mLocalZone;
  private SeekBar mSeekBar = null;
  private Calendar mTimeAvailableEnd = null;
  private Calendar mTimeAvailableStart = null;
  private Calendar mTimeEnd = null;
  private Calendar mTimeStart = null;
  private Calendar mTimeVirtualPosition = null;
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
    mTimeEnd = null;
    mTimeStart = null;
    mRpcComplete = false;
  }

  /** After any given RPC completes, check state (given all) and continue. */
  private void rpcComplete() {
    if (mRpcComplete) return;

    if (mLocalZone == null) return;
    if (mTimeStart == null) return;
    if (mTimeEnd == null) return;
    if (mTimeAvailableStart == null) return;
    if (mTimeAvailableEnd == null) return;
    if (mTimeVirtualPosition == null) return;
    if (!mHavePlaybackInfo) return;

    // Shift start/end times into local zones.
    mTimeStart.add(Calendar.MILLISECOND, mLocalZone.getRawOffset());
    mTimeEnd.add(Calendar.MILLISECOND, mLocalZone.getRawOffset());
    // Set labels with those correct values.
    setSeekbarLabels();

    // Turn off loading indicator, reveal content.
    ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
        .setDisplayedChild(0);

    mRpcComplete = true;

    // We might have aborted this, if PlaybackInfo finished before last.
    setSeekbarPositions();
  }

  /** Set the start/end time labels of the seek bar. */
  private void setSeekbarLabels() {
    final TextView startLabel =
        (TextView) findViewById(R.id.content_start_time);
    final TextView endLabel =
        (TextView) findViewById(R.id.content_end_time);

    if (mContentType == ContentType.LIVE) {
      // Show absolute start and end time.
      startLabel.setVisibility(View.VISIBLE);
      startLabel.setText(mDisplayTimeFormat.format(mTimeStart.getTime()));
      endLabel.setText(mDisplayTimeFormat.format(mTimeEnd.getTime()));
    } else if (mContentType == ContentType.RECORDING) {
      // Just show duration as end time.
      startLabel.setVisibility(View.GONE);
      final Calendar dur = Calendar.getInstance(mGmtZone);
      dur.setTimeInMillis(
          mTimeEnd.getTimeInMillis() - mTimeStart.getTimeInMillis());
      endLabel.setText(mDisplayTimeFormat.format(dur.getTime()));
    }
  }

  /** Set the current, and available start/end positions, of the seek bar. */
  private void setSeekbarPositions() {
    if (!mRpcComplete) return;

    int max = (int) (
        mTimeEnd.getTimeInMillis() - mTimeStart.getTimeInMillis());
    int progress = (int) (
        mTimeVirtualPosition.getTimeInMillis() - mTimeStart.getTimeInMillis());

    // TDOO: Indicate available range.

    mSeekBar.setMax(max);
    mSeekBar.setProgress(progress);
  }
}
