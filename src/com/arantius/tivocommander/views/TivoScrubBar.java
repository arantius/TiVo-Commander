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
  /** The max value for the available section of the recording. */
  protected int mAvailableMax;
  /** The min value for the available section of the recording. */
  protected int mAvailableMin;
  /** The maximum point possible within this recording. */
  protected int mMax;
  /** The current point of playback; between available range. */
  protected int mProgress;

  private Bitmap mBitmapBarEndLeft;
  private Bitmap mBitmapBarEndRight;
  private Bitmap mBitmapBarMid;
  private Bitmap mBitmapThumb;

  private Paint mPaintActiveSection;
  private Paint mPaintTextPaint;

  final private int mSizeActivePad = 54;
  final private int mSizeBarEndWid = 65;
  final private int mSizeHeiBar = 36;
  final private int mSizeHeiBarPad = 10;
  final private int mSizeThumbPad = 27;
  final private int mSizeViewHei = 56;
  private int mSizeViewWid;

  final private int mCoordBarLeft = mSizeBarEndWid;
  private int mCoordActiveTop = 15;
  private int mCoordActiveBot = 42;
  private int mCoordBarRight;
  final private int mCoordThumbTop = 3;
  private int mCoordThumbLeft = 250 - mSizeThumbPad;

  private Rect mRectActive = new Rect(200, mCoordActiveTop, 300, mCoordActiveBot);
  private Rect mRectBarMid = new Rect();

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
    mPaintActiveSection.setColor(0xFF5EFF44); // Green?

    mPaintTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaintTextPaint.setColor(0xFFFFFFFF); // White?
  }

  @Override
  protected void onDraw(Canvas canvas) {
    // Draw the green active region, first, under the bar background, so the
    // edges show through, rounded.
    canvas.drawRect(mRectActive, mPaintActiveSection);

    // TODO: Draw the 15-min hash marks.

    // Draw the background of the bar.
    canvas.drawBitmap(mBitmapBarEndLeft, 0, mSizeHeiBarPad, null);
    canvas.drawBitmap(mBitmapBarEndRight, mSizeViewWid - mSizeBarEndWid,
        mSizeHeiBarPad, null);
    canvas.drawBitmap(mBitmapBarMid, null, mRectBarMid, null);

    // Draw the thumb.
    canvas.drawBitmap(mBitmapThumb, mCoordThumbLeft, mCoordThumbTop, null);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // Force fill_parent width, fixed content height (by images).
    mSizeViewWid = MeasureSpec.getSize(widthMeasureSpec);
    this.setMeasuredDimension(mSizeViewWid, mSizeViewHei);

    // Fix the rect which scales the middle of our bar, with this width info.
    mCoordBarRight = mSizeViewWid - mSizeBarEndWid;
    mRectBarMid.set(mSizeBarEndWid, mSizeHeiBarPad, mCoordBarRight,
        mSizeHeiBarPad + mSizeHeiBar);
  }

  /** Set all the properties that affect the range this view presents. */
  public void setRange(int availableMin, int progress, int availableMax, int max) {
    boolean change = false;

    // Save inputs.
    //@formatter:off
    if (mAvailableMin != availableMin) change = true;
    this.mAvailableMin = availableMin;
    if (mProgress != progress) change = true;
    this.mProgress = progress;
    if (mAvailableMax != availableMax) change = true;
    this.mAvailableMax = progress;
    if (mMax != max) change = true;
    this.mMax = max;
    //@formatter:on

    if (!change) {
      return;
    }

    // Derive layout from inputs.
    float scale = (float) (mSizeViewWid - mSizeActivePad * 2) / max;
    mCoordThumbLeft = (int) Math.floor(
        mCoordBarLeft + (progress * scale) - mSizeThumbPad);
    mRectActive.set(
        mSizeActivePad + (int) (availableMin * scale),
        mCoordActiveTop,
        mSizeActivePad + (int) (availableMax * scale),
        mCoordActiveBot);

    // Schedule a redraw.
    invalidate();
    requestLayout();
  }
}
