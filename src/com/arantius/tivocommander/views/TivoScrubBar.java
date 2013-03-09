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

package com.arantius.tivocommander.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.arantius.tivocommander.R;

/** A scrub bar to display/control playback like the native TiVo interface. */
public class TivoScrubBar extends View {
  /** The max value for the available section of the recording; milliseconds. */
  protected int mAvailableMax = 2400000;
  /** The min value for the available section of the recording; milliseconds. */
  protected int mAvailableMin = 600000;
  /** The maximum point possible within this recording; milliseconds. */
  protected int mMax = 5400000;
  /** The current point of playback; milliseconds; inside available range. */
  protected int mProgress = 1700000;
  /** The time label to display on the left side. */
  protected String mLabelLeft = "";
  /** The time label to display on the right side. */
  protected String mLabelRight = "1h 30m";

  private Bitmap mBitmapBarEndLeft;
  private Bitmap mBitmapBarEndRight;
  private Bitmap mBitmapBarMid;
  private Bitmap mBitmapThumb;

  private Paint mPaintActiveSection;
  private Paint mPaintHash;
  private Paint mPaintText;

  final private int mSizeHashWid = 4;
  final private int mSizeHeiBar = 36;
  final private int mSizeHeiBarPad = 10;
  final private int mSizeTextPadSide = 20;
  final private int mSizeTextPadTop = 25;
  final private int mSizeTextHei = 22;
  final private int mSizeThumb = 55;
  final private int mSizeThumbPad = 27;
  final private int mSizeViewHei = 56;
  private int mSizeViewWid;

  private float mScaleHei = 1;

  final private int mCoordActiveTop = 12;
  final private int mCoordActiveBot = 45;
  final private int mCoordThumbTop = 2;
  private int mCoordThumbLeft = 0;

  private Rect mRectActive = new Rect(0, mCoordActiveTop, 0, mCoordActiveBot);
  private Rect mRectBarEndLeft = new Rect();
  private Rect mRectBarEndRight = new Rect();
  private Rect mRectBarMid = new Rect();
  private Rect mRectHash = new Rect();
  private Rect mRectThumb = new Rect();

  public TivoScrubBar(Context context) {
    this(context, null);
  }

  public TivoScrubBar(Context context, AttributeSet attrs) {
    super(context, attrs);

    mBitmapBarEndLeft =
        BitmapFactory.decodeResource(getResources(), R.drawable.scrub_left);
    mBitmapBarEndRight =
        BitmapFactory.decodeResource(getResources(), R.drawable.scrub_right);
    mBitmapBarMid =
        BitmapFactory.decodeResource(getResources(), R.drawable.scrub_mid);
    mBitmapThumb =
        BitmapFactory.decodeResource(getResources(), R.drawable.scrub_thumb);

    mPaintActiveSection = new Paint();
    mPaintActiveSection.setStyle(Paint.Style.FILL);
    mPaintActiveSection.setColor(0xFF348E26);

    mPaintHash = new Paint();
    mPaintHash.setStyle(Paint.Style.FILL);
    mPaintHash.setColor(0xFFFFFFFF);

    mPaintText = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaintText.setColor(0xFFFFFFFF);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    // Calculate the time labels and their sizes.
    mPaintText.setTextSize(mSizeTextHei * mScaleHei);
    if (mLabelLeft == null) {
      mLabelLeft = "";
    }
    int widLabelLeft =
        mSizeTextPadSide + (int) mPaintText.measureText(mLabelLeft);
    if (mLabelRight == null) {
      mLabelRight = "";
    }
    int widLabelRight =
        mSizeTextPadSide + (int) mPaintText.measureText(mLabelRight);

    // Calculate the rects for the bar pieces, based on label sizes.
    final int barTop = (int) (mSizeHeiBarPad * mScaleHei);
    final int barBot = (int) (barTop + (mSizeHeiBar * mScaleHei));
    mRectBarEndLeft.set(0, barTop, Math.max(16, widLabelLeft), barBot);
    mRectBarEndRight.set(mSizeViewWid - Math.max(16, widLabelRight), barTop,
        mSizeViewWid, barBot);
    mRectBarMid.set(mRectBarEndLeft.right, barTop, mRectBarEndRight.left,
        barBot);

    // Calculate the position of the active region and thumb.
    float scale = (float) (mRectBarMid.right - mRectBarMid.left) / mMax;
    mCoordThumbLeft =
        (int) Math.floor(mRectBarMid.left + (mProgress * scale)
            - (int) (mSizeThumbPad * mScaleHei));
    //@formatter:off
    mRectActive.set(
        mRectBarEndLeft.right + (int) (mAvailableMin * scale),
        (int) Math.ceil(mCoordActiveTop * mScaleHei),
        mRectBarEndLeft.right + (int) (mAvailableMax * scale),
        (int) (mCoordActiveBot * mScaleHei));
    //@formatter:on

    // Draw the green active region, first, under the bar outline, so the
    // edges show through, rounded.
    canvas.drawRect(mRectActive, mPaintActiveSection);

    // Draw the hash marks.
    int hashWid = 15 * 60 * 1000;
    // TODO: Does this limit match TiVo on screen behavior?
    while (true) {
      // Minus 30k so that we don't draw a hash right at the end. Not minus
      // a full minute though, because the TiVo itself will e.g. draw the
      // 1-hour hash for a 61 min. recording.
      int numHashes = (mMax - 30000) / hashWid;
      if (numHashes <= 10)
        break;
      hashWid *= 2;
    }
    for (int x = hashWid; x < mMax - 30000; x += hashWid) {
      int hashLeft = mRectBarMid.left + (int) (x * scale) - (mSizeHashWid / 2);
      //@formatter:off
      mRectHash.set(
          hashLeft,
          (int) Math.ceil(mCoordActiveTop * mScaleHei),
          hashLeft + mSizeHashWid,
          (int) (mCoordActiveBot * mScaleHei));
      canvas.drawRect(mRectHash, mPaintHash);
    }

    // Draw the outline of the bar.
    canvas.drawBitmap(mBitmapBarEndLeft, null, mRectBarEndLeft, null);
    canvas.drawBitmap(mBitmapBarEndRight, null, mRectBarEndRight, null);
    canvas.drawBitmap(mBitmapBarMid, null, mRectBarMid, null);

    // Draw the time labels.
    //@formatter:off
    canvas.drawText(
        mLabelLeft,
        mRectBarEndLeft.left + (mSizeTextPadSide / 2),
        mRectBarEndLeft.top + (mSizeTextPadTop * mScaleHei),
        mPaintText);
    canvas.drawText(
        mLabelRight,
        mRectBarEndRight.left + (mSizeTextPadSide / 2),
        mRectBarEndRight.top + (mSizeTextPadTop * mScaleHei),
        mPaintText);
    //@formatter:on

    // Draw the thumb.
    //@formatter:off
    mRectThumb.set(
        mCoordThumbLeft,
        (int) (mCoordThumbTop * mScaleHei),
        mCoordThumbLeft + (int) (mSizeThumb * mScaleHei),
        (int) (mCoordThumbTop + mSizeThumb * mScaleHei));
    //@formatter:on
    canvas.drawBitmap(mBitmapThumb, null, mRectThumb, null);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // Force fill_parent width, fixed content height (by images).
    mSizeViewWid = MeasureSpec.getSize(widthMeasureSpec);
    int viewHei = MeasureSpec.getSize(heightMeasureSpec);
    mScaleHei = (float) viewHei / mSizeViewHei;
    this.setMeasuredDimension(mSizeViewWid, viewHei);
  }

  /** Set all the properties that affect the range this view presents. */
  public void update(int availableMin, int progress, int availableMax, int max,
      String labelLeft, String labelRight) {
    boolean change = false;

    // Save inputs.
    //@formatter:off
    if (mAvailableMin != availableMin) change = true;
    this.mAvailableMin = availableMin;
    if (mProgress != progress) change = true;
    this.mProgress = progress;
    if (mAvailableMax != availableMax) change = true;
    this.mAvailableMax = availableMax;
    if (mMax != max) change = true;
    this.mMax = max;
    if (mLabelLeft != labelLeft) change = true;
    this.mLabelLeft = labelLeft;
    if (mLabelRight != labelRight) change = true;
    this.mLabelRight = labelRight;
    //@formatter:on

    if (!change) {
      return;
    }

    // Schedule a redraw.
    invalidate();
    requestLayout();
  }
}
