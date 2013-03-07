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
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

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
import com.arantius.tivocommander.rpc.request.RecordingSearch;
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
  private final MindRpcResponseListener mOfferCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode offer = response.getBody().path("offer").path(0);
//          Utils.log(Utils.stringifyToPrettyJson(offer));

          Date now = new Date();
          Utils.log("offer; current time       " + mDateFormat.format(now));

          setMembersFromContent(offer, offer.path("startTime").asText());

          setSeekbarLabels();
          rpcComplete();
        };
      };
  private final MindRpcResponseListener mPlaybackInfoCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
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
  private final MindRpcResponseListener mRecordingCallback =
      new MindRpcResponseListener() {
        @SuppressWarnings("unused")
        public void onResponse(MindRpcResponse response) {
          JsonNode recording = response.getBody().path("recording").path(0);
//          Utils.log(Utils.stringifyToPrettyJson(recording));

          if (true) {  // Just here to enable for debugging.
            ObjectNode filteredContent = (ObjectNode) recording;
            filteredContent.remove("category");
            filteredContent.remove("credit");
            filteredContent.remove("image");
            Utils.log(Utils.stringifyToPrettyJson((JsonNode) filteredContent));
          }

          Date now = new Date();
          Utils.log("recording; current time     " + mDateFormat.format(now));

          setMembersFromContent(
              recording, recording.path("actualStartTime").asText());

          setSeekbarLabels();
          rpcComplete();
        }
      };
  private final MindRpcResponseListener mWhatsOnCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode whatsOn = response.getBody().path("whatsOn").path(0);
//          Utils.log(Utils.stringifyToPrettyJson(whatsOn));

          String playbackType = whatsOn.path("playbackType").asText();
          String contentId = null;
          if ("recording".equals(playbackType)) {
            contentId = whatsOn.path("recordingId").asText();
          } else if ("liveCache".equals(playbackType)) {
            contentId = whatsOn.path("offerId").asText();
          } else {
            Utils.logError("Unsupported playbackType: " + playbackType);
            return;
          }

          if (mContentId != null) {
            // Ignore extra callbacks where the content has not changed.
            if (mContentId.equals(contentId)) {
              return;
            }
            // Otherwise, start over with new requests and data.
            initInstanceVars();
          }
          mContentId = contentId;
          ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
              .setDisplayedChild(1);

          if ("recording".equals(playbackType)) {
            mContentType = ContentType.RECORDING;
            RecordingSearch request = new RecordingSearch(contentId);
            request.setLevelOfDetail("low");
            MindRpc.addRequest(request, mRecordingCallback);
          } else if ("liveCache".equals(playbackType)) {
            mContentType = ContentType.LIVE;
            OfferSearch request = new OfferSearch("offerId", contentId);
            MindRpc.addRequest(request, mOfferCallback);
          }
        };
      };

  final private SimpleDateFormat mDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", Locale.US);
  final private SimpleDateFormat mDisplayTimeFormat = new SimpleDateFormat(
      "hh:mm", Locale.US);
  private Integer mGmtOffsetMillis = null;

  private ContentType mContentType = null;
  private SeekBar mSeekBar = null;
  private Long mMillisContentBegin = null;
  private Long mMillisContentEnd = null;
  private Long mMillisPosition = null;
  private Long mMillisRecordingBegin = null;
  private Long mMillisRecordingEnd = null;
  private Long mMillisVirtualPosition = null;
  private String mContentId = null;
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
    mMillisContentBegin = null;
    mMillisContentEnd = null;
    mMillisPosition = null;
    mMillisRecordingBegin = null;
    mMillisRecordingEnd = null;
    mMillisVirtualPosition = null;
    mRpcComplete = false;
  }

  private void setMembersFromContent(JsonNode content, String startTimeStr) {
    // Set title and subtitle.
    ((TextView) findViewById(R.id.content_title)).setText(content.path(
        "title").asText());
    String subtitle = content.path("subtitle").asText();
    TextView subtitleView = (TextView) findViewById(R.id.content_subtitle);
    if (null == subtitle || "".equals(subtitle)) {
      subtitleView.setVisibility(View.GONE);
    } else {
      subtitleView.setVisibility(View.VISIBLE);
      subtitleView.setText(subtitle);
    }

    Utils.log(String.format(Locale.US,
        "Time from content:          %s; %d",
        startTimeStr, content.path("duration").asLong()));

    // Calculate absolute start and end times.
    final Integer durationSec = content.path("duration").asInt();
    try {
      Date beginDate = mDateFormat.parse(startTimeStr);
      mMillisContentBegin = beginDate.getTime();
    } catch (ParseException e) {
      Utils.logError(
          "Failed to parse start time " + startTimeStr, e);
      mMillisContentBegin = 0L;
    }
    mMillisContentEnd = mMillisContentBegin + (durationSec * 1000);

    Utils.log(String.format(Locale.US,
        "content begin %d %s",
        mMillisContentBegin, mDateFormat.format(new Date(mMillisContentBegin))
        ));
    Utils.log(String.format(Locale.US,
        "content end   %d %s",
        mMillisContentEnd, mDateFormat.format(new Date(mMillisContentEnd))
        ));
  }

  /** After any given RPC completes, check state (given all) and continue. */
  private void rpcComplete() {
    if (mRpcComplete) return;

    if (mGmtOffsetMillis == null) return;
    if (mMillisContentBegin == null) return;
    if (mMillisContentEnd == null) return;
    if (mMillisPosition == null) return;
    if (mMillisRecordingBegin == null) return;
    if (mMillisRecordingEnd == null) return;
    if (mMillisVirtualPosition == null) return;

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
      Date beginDate = new Date(mMillisContentBegin + mGmtOffsetMillis);
      Date endDate = new Date(mMillisContentEnd + mGmtOffsetMillis);

      // Show absolute start and end time.
      startLabel.setVisibility(View.VISIBLE);
      startLabel.setText(mDisplayTimeFormat.format(beginDate));
      endLabel.setText(mDisplayTimeFormat.format(endDate));
    } else if (mContentType == ContentType.RECORDING) {
      Date durationDate = new Date(mMillisContentEnd - mMillisContentBegin);
      // Just show duration as end time.
      startLabel.setVisibility(View.GONE);
      endLabel.setText(mDisplayTimeFormat.format(durationDate));
    }
  }

  /** Set the current, and available start/end positions, of the seek bar. */
  private void setSeekbarPositions() {
    if (!mRpcComplete) return;

    int activeMin = 0;
    int progress = 0;
    int activeMax = 0;
    int max = 0;
    if (mContentType == ContentType.LIVE) {
      max = (int) (mMillisContentEnd - mMillisContentBegin);
      progress = (int) (mMillisVirtualPosition - mMillisContentBegin);
      activeMin = (int) (progress - mMillisPosition);
      activeMax = (int) (activeMin + mMillisRecordingEnd);
    } else {
      // TODO: How do I handle if the recording began late?
      max = (int) (mMillisContentEnd - mMillisContentBegin);
      // TODO: Real/safe cast to int?
      progress = (int) (mMillisPosition - 0);
      activeMin = (int) (mMillisRecordingBegin - 0);
      activeMax = (int) (mMillisRecordingEnd - 0);
    }


    // Live shows sometime have a bit of the previous show recorded also.
    // The TiVo displays a seek bar with the whole recorded range; for now,
    // we just truncate for simplicity.
    if (activeMin < 0) {
      activeMax += activeMin;
      activeMin = 0;
    }

    // For testing, until I get a real View set up, log an ascii-art bar.
    float scale = 60 / (float) max;
    char[] bar = new char[60];
    Arrays.fill(bar, ' ');
    for (int i = (int) (activeMin * scale); i < Math.floor(activeMax * scale); i++) {
      bar[i] = '*';
    }
    bar[0] = '|';
    bar[59] = '|';
    bar[(int)(progress * scale)] = 'O';
    Utils.log(new String(bar));

    Utils.log(String.format(
        Locale.US,
        "offer [%,d %,d %,d] %,d %,d",
        mMillisContentBegin, mMillisVirtualPosition, mMillisContentEnd,
        mMillisContentEnd - mMillisContentBegin,
        mMillisContentEnd - mMillisVirtualPosition
        ));
    Utils.log(String.format(
        Locale.US,
        "record [%,d %,d %,d] %,d offset: %,d",
        mMillisRecordingBegin, mMillisPosition, mMillisRecordingEnd,
        mMillisRecordingEnd - mMillisRecordingBegin, mGmtOffsetMillis
        ));
    Utils.log(String.format(
        Locale.US,
        "calc %,d %,d %,d %,d",
        activeMin, progress, activeMax, max
        ));

    mSeekBar.setMax(max);
    mSeekBar.setProgress(progress);
  }
}
