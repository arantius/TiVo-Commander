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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.BodyConfigSearch;
import com.arantius.tivocommander.rpc.request.OfferSearch;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.request.VideoPlaybackInfoEventRegister;
import com.arantius.tivocommander.rpc.request.WhatsOnSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.arantius.tivocommander.views.TivoScrubBar;

public class NowShowing extends Activity {
  private enum ContentType {
    LIVE, RECORDING;
  }

  private class PlaybackRange {
    public long absoluteBegin;
    public long absoluteEnd;
    public int activeMin;
    public int progress;
    public int activeMax;
    public int max;
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

  private String mCollectionId = null;
  private String mContentId = null;
  private ContentType mContentType = null;
  final private SimpleDateFormat mDateFormat = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", Locale.US);
  final private SimpleDateFormat mDisplayTimeFormat = new SimpleDateFormat(
      "h:mm", Locale.US);
  private String mOfferId = null;
  private String mRecordingId = null;
  private String mWhatsOnId = null;

  private Integer mGmtOffsetMillis = null;
  private Long mMillisActualBegin = null;
  private Long mMillisContentBegin = null;
  private Long mMillisContentEnd = null;
  private Integer mMillisPosition = null;
  private Integer mMillisRecordingBegin = null;
  private Integer mMillisRecordingEnd = null;
  private Long mMillisVirtualPosition = null;
  private boolean mRpcComplete = false;
  private TivoScrubBar mScrubBar = null;

  private final MindRpcResponseListener mOfferCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode offer = response.getBody().path("offer").path(0);
          setTitleFromContent(offer);

          final Integer durationSec = offer.path("duration").asInt();
          final String startTimeStr = offer.path("startTime").asText();
          try {
            Date beginDate = mDateFormat.parse(startTimeStr);
            mMillisContentBegin = beginDate.getTime();
          } catch (ParseException e) {
            Utils.logError("Failed to parse start time " + startTimeStr, e);
            mMillisContentBegin = 0L;
          }
          mMillisContentEnd = mMillisContentBegin + (durationSec * 1000);

          rpcComplete();
        };
      };
  private final MindRpcResponseListener mPlaybackInfoCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode playbackInfo = response.getBody();

          mMillisPosition = playbackInfo.path("position").asInt();
          mMillisRecordingBegin = playbackInfo.path("begin").asInt();
          mMillisRecordingEnd = playbackInfo.path("end").asInt();
          mMillisVirtualPosition =
              playbackInfo.path("virtualPosition").asLong();

          rpcComplete();

          // Update these every time, not just the first via rpcComplete().
          setScrubBar();
        };
      };
  private final MindRpcResponseListener mRecordingCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode recording = response.getBody().path("recording").path(0);

          setTitleFromContent(recording);

          // The time that this recording actually started.
          final String actualBeginTimeStr =
              recording.path("actualStartTime").asText();
          try {
            mMillisActualBegin =
                mDateFormat.parse(actualBeginTimeStr).getTime();
          } catch (ParseException e) {
            Utils.logError("Failed to parse start time " + actualBeginTimeStr,
                e);
            mMillisActualBegin = 0L;
          }

          // The time this recording (content?) was scheduled to begin.
          final String beginTimeStr =
              recording.path("scheduledStartTime").asText();
          try {
            mMillisContentBegin = mDateFormat.parse(beginTimeStr).getTime();
          } catch (ParseException e) {
            Utils.logError("Failed to parse start time " + beginTimeStr, e);
            mMillisContentBegin = 0L;
          }

          // The time this recording (content?) was scheduled to end.
          final String endTimeStr = recording.path("scheduledEndTime").asText();
          try {
            final Date endDate = mDateFormat.parse(endTimeStr);
            mMillisContentEnd = endDate.getTime();
          } catch (ParseException e) {
            Utils.logError("Failed to parse start time " + endTimeStr, e);
            mMillisContentEnd = 0L;
          }

          rpcComplete();
        }
      };

  private final MindRpcResponseListener mWhatsOnCallback =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          JsonNode whatsOn = response.getBody().path("whatsOn").path(0);

          mCollectionId = whatsOn.path("collectionId").asText();
          mContentId = whatsOn.path("contentId").asText();
          mOfferId = whatsOn.path("offerId").asText();
          mRecordingId = whatsOn.path("recordingId").asText();

          String playbackType = whatsOn.path("playbackType").asText();
          String whatsOnId = null;
          if ("recording".equals(playbackType)) {
            whatsOnId = whatsOn.path("recordingId").asText();
          } else if ("liveCache".equals(playbackType)) {
            whatsOnId = whatsOn.path("offerId").asText();
          } else {
            Utils.logError("Unsupported playbackType: " + playbackType);
            return;
          }

          if (mWhatsOnId != null) {
            // Ignore extra callbacks where the content has not changed.
            if (mWhatsOnId.equals(whatsOnId)) {
              return;
            }
            // Otherwise, start over with new requests and data.
            initInstanceVars();
          }
          mWhatsOnId = whatsOnId;
          ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
              .setDisplayedChild(1);

          if ("recording".equals(playbackType)) {
            mContentType = ContentType.RECORDING;
            RecordingSearch request = new RecordingSearch(whatsOnId);
            request.setLevelOfDetail("low");
            MindRpc.addRequest(request, mRecordingCallback);
          } else if ("liveCache".equals(playbackType)) {
            mContentType = ContentType.LIVE;
            OfferSearch request = new OfferSearch("offerId", whatsOnId);
            MindRpc.addRequest(request, mOfferCallback);
          }
        };
      };

  /**
   * Calculate everything about the range we should display.  Absolute start
   * and end times of the whole bar, the active range that is recorded, and
   * the progress through that range.
   */
  private PlaybackRange getPlaybackRange() {
    final int millisHalfHour = 1000 * 60 * 30;
    PlaybackRange range = new PlaybackRange();

    range.absoluteBegin = mMillisContentBegin;
    range.absoluteEnd = mMillisContentEnd;
    range.max = (int) (mMillisContentEnd - mMillisContentBegin);

    if (mContentType == ContentType.LIVE) {
      // TODO: Recording starts 1/2 hour+ after absolute begin.
      range.progress = (int) (mMillisVirtualPosition - mMillisContentBegin);
      range.activeMin = range.progress - mMillisPosition;
      range.activeMax = range.activeMin + mMillisRecordingEnd;

      // If the active range is too far from the content beginning, shrink
      // the range to hold it.
      while (range.activeMin > millisHalfHour) {
        range.absoluteBegin += millisHalfHour;
        range.activeMin -= millisHalfHour;
        range.progress -= millisHalfHour;
        range.activeMax -= millisHalfHour;
        range.max -= millisHalfHour;
      }
    } else {
      range.activeMin = (int) (mMillisActualBegin - mMillisContentBegin);
      range.activeMax = range.activeMin + mMillisRecordingEnd;
      range.progress = range.activeMin + mMillisPosition;
    }

    // If the recording extends beyond the scheduled period (by more than half
    // a minute) extend the scrub bar by half an hour.
    // TODO: Do these need to be whiles in case of big padding?
    if (range.activeMin < -30000) {
      Utils.log(String.format(Locale.US,
          "Adjusting beginning back becase %d < 0", range.activeMin));
      range.absoluteBegin -= millisHalfHour;
      // Since min must be 0, actually shift everything else forward.
      range.max += millisHalfHour;
      range.progress += millisHalfHour;
      range.activeMin += millisHalfHour;
      range.activeMax += millisHalfHour;
    } else if (range.activeMin < 0) {
      range.activeMin = 0;
    }
    if (range.activeMax > range.max + 30000) {
      Utils.log(String.format(Locale.US,
          "Adjusting end forward becase %d > %d", range.activeMax, range.max));
      range.absoluteEnd += millisHalfHour;
      range.max += millisHalfHour;
    } else if (range.activeMax > range.max) {
      range.activeMax = range.max;
    }

    return range;
  }

  /** (Re-)Initialize instance variables. */
  private void initInstanceVars() {
    mCollectionId = null;
    mContentId = null;
    mContentType = null;
    mMillisActualBegin = null;
    mMillisContentBegin = null;
    mMillisContentEnd = null;
    mOfferId = null;
    mRecordingId = null;
    mRpcComplete = false;
    mWhatsOnId = null;
  }

  public void doExplore(View unused) {
    Utils.log("NowShowing::doExplore()");

    Intent intent = new Intent(this, ExploreTabs.class);
    if (mCollectionId != null && !"".equals(mCollectionId)) {
      intent.putExtra("collectionId", mCollectionId);
    }
    if (mContentId != null && !"".equals(mContentId)) {
      intent.putExtra("contentId", mContentId);
    }
    if (mOfferId != null && !"".equals(mOfferId)) {
      intent.putExtra("offerId", mOfferId);
    }
    if (mRecordingId != null && !"".equals(mRecordingId)) {
      intent.putExtra("recordingId", mRecordingId);
    }

    startActivity(intent);
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
    case R.id.target_season_pass:
      intent = new Intent(getBaseContext(), SeasonPass.class);
      break;
    case R.id.target_settings:
      intent = new Intent(getBaseContext(), Discover.class);
      break;
    case R.id.target_todo:
      intent = new Intent(getBaseContext(), ToDo.class);
      break;
    }

    if (intent != null) {
      startActivity(intent);
    }
  }

  public void onClickRemote(View v) {
    MindRpc.addRequest(Remote.viewIdToEvent(v.getId()), null);
  }

  @SuppressLint("InlinedApi")
  public final boolean onCreateOptionsMenu(Menu menu) {
    Utils.addToMenu(menu, this, R.id.menu_item_help, R.drawable.icon_help,
        "Help", MenuItem.SHOW_AS_ACTION_NEVER);
    Utils.addToMenu(menu, this, R.id.menu_item_about, R.drawable.icon_info,
        "About", MenuItem.SHOW_AS_ACTION_NEVER);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:NowShowing");
    MindRpc.cancelAll();
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:NowShowing");

    if (MindRpc.init(this, null)) {
      return;
    }

    setContentView(R.layout.now_showing);
    setTitle("Now Showing");

    mScrubBar = (TivoScrubBar) findViewById(R.id.tivo_scrub_bar);
    TimeZone gmtTz = TimeZone.getTimeZone("GMT");
    mDateFormat.setTimeZone(gmtTz);

    // Turn on loading indicator.
    ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
        .setDisplayedChild(1);
    initInstanceVars();

    MindRpc.addRequest(new BodyConfigSearch(), mBodyConfigCallback);

    WhatsOnSearch whatsOnRequest = new WhatsOnSearch();
    MindRpc.addRequest(whatsOnRequest, mWhatsOnCallback);

    VideoPlaybackInfoEventRegister playbackInfoRequest =
        new VideoPlaybackInfoEventRegister(1001);
    MindRpc.addRequest(playbackInfoRequest, mPlaybackInfoCallback);
  }

  /** After any given RPC completes, check state (given all) and continue. */
  private void rpcComplete() {
    // @formatter:off
    if (mRpcComplete) return;

    if (mGmtOffsetMillis == null) return;
    if (mMillisContentBegin == null) return;
    if (mMillisContentEnd == null) return;
    if (mContentType == ContentType.RECORDING) {
      if (mMillisActualBegin == null) return;
    }
    if (mMillisPosition == null) return;
    if (mMillisRecordingBegin == null) return;
    if (mMillisRecordingEnd == null) return;
    if (mMillisVirtualPosition == null) return;
    // @formatter:on

    mRpcComplete = true;

    // Run this for sure once.
    setScrubBar();
    // Then make them visible.
    ((ViewFlipper) findViewById(R.id.now_showing_detail_flipper))
        .setDisplayedChild(0);
  }

  /** Set the start/end time labels of the seek bar. */
  private void setScrubBar() {
    if (!mRpcComplete) {
      return;
    }
    PlaybackRange range = getPlaybackRange();

    String labelLeft = null;
    String labelRight = null;

    if (mContentType == ContentType.LIVE) {
      // Show absolute start and end time.
      labelLeft = mDisplayTimeFormat.format(new Date(range.absoluteBegin));
      labelRight = mDisplayTimeFormat.format(new Date(range.absoluteEnd));
    } else if (mContentType == ContentType.RECORDING) {
      final int millis = (int) (mMillisContentEnd - mMillisContentBegin);
      int minutes = (int) Math.ceil((millis) / (1000 * 60));
      labelRight = "";
      if (minutes >= 60) {
        labelRight += String.format(Locale.US, "%dh", (minutes / 60));
      }
      minutes %= 60;
      if (minutes != 0) {
        if (!"".equals(labelRight)) {
          labelRight += " ";
        }
        labelRight += String.format(Locale.US, "%dm", minutes);
      }
    }

    mScrubBar.update(range.activeMin, range.progress, range.activeMax,
        range.max, labelLeft, labelRight);
  }

  private void setTitleFromContent(JsonNode content) {
    String title = content.path("title").asText();
    String movieYear = content.path("movieYear").asText();
    if (movieYear != null && !"".equals(movieYear)) {
      title += " (" + movieYear + ")";
    }
    ((TextView) findViewById(R.id.content_title)).setText(title);

    String subtitle = content.path("subtitle").asText();
    TextView subtitleView = (TextView) findViewById(R.id.content_subtitle);
    if (null == subtitle || "".equals(subtitle)) {
      subtitleView.setVisibility(View.GONE);
    } else {
      subtitleView.setVisibility(View.VISIBLE);
      subtitleView.setText(subtitle);
    }
  }
}
