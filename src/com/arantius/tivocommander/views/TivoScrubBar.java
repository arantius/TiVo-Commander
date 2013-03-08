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

import com.arantius.tivocommander.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

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
  private Paint mPaintBarEndLeft;
  private Paint mPaintBarEndRight;
  private Paint mPaintBar;
  private Paint mPaintTextPaint;
  private Paint mPaintThumb;

  private Rect mRectBarMid = new Rect();

  final private int mSizeBarEndWid = 65;
  private int mSizeBarWid = 0;
  final private int mSizeHeiBar = 36;
  final private int mSizeHeiBarPad = 10;
  final private int mSizeThumbPad = 27;
  final private int mSizeThumbHei = 55;
  final private int mSizeThumbWid = 55;
  final private int mSizeViewHei = 56;
  private int mSizeViewWid;

  final private int mCoordBarLeft = mSizeBarEndWid;
  private int mCoordBarRight;
  private int mCoordThumbLeft = mSizeBarEndWid - mSizeThumbPad;

  public TivoScrubBar(Context context) {
    this(context, null);
  }

  public TivoScrubBar(Context context, AttributeSet attrs) {
    super(context, attrs);

    // TODO: Custom attrs if necessary.
    // http://developer.android.com/training/custom-views/create-view.html#applyattr

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
    mPaintActiveSection.setColor(0xFF338B25); // Green?

    mPaintTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaintTextPaint.setColor(0xFFFFFFFF); // White?
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawBitmap(mBitmapBarEndLeft, 0, mSizeHeiBarPad, null);
    canvas.drawBitmap(mBitmapBarEndRight, mSizeViewWid - mSizeBarEndWid,
        mSizeHeiBarPad, null);
    canvas.drawBitmap(mBitmapBarMid, null, mRectBarMid, null);

    canvas.drawBitmap(mBitmapThumb, mCoordThumbLeft, 0, null);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    // Force fill_parent width, fixed content height (by images).
    mSizeViewWid = MeasureSpec.getSize(widthMeasureSpec);
    mSizeBarWid = mSizeViewWid - (mSizeBarEndWid * 2);
    this.setMeasuredDimension(mSizeViewWid, mSizeViewHei);

    // Fix the rect which scales the middle of our bar, with this width info.
    mCoordBarRight = mSizeViewWid - mSizeBarEndWid;
    mRectBarMid.set(mSizeBarEndWid, mSizeHeiBarPad, mCoordBarRight, mSizeHeiBarPad
        + mSizeHeiBar);
  }

  /** Set all the properties that affect the range this view presents. */
  public void setRange(int availableMin, int progress, int availableMax, int max) {
    boolean doDraw = false;

    // Save inputs.
    //@formatter:off
    if (mAvailableMin != availableMin) doDraw = true;
    this.mAvailableMin = availableMin;
    if (mProgress != progress) doDraw = true;
    this.mProgress = progress;
    if (mAvailableMax != availableMax) doDraw = true;
    this.mAvailableMax = progress;
    if (mMax != max) doDraw = true;
    this.mMax = max;
    //@formatter:on

    // Derive layout from inputs.
    float scale = (float) mSizeBarWid / max;
    mCoordThumbLeft = (int) Math.floor(
        mSizeBarEndWid + (progress * scale) - mSizeThumbPad);

    if (doDraw) {
      invalidate();
      requestLayout();
    }
  }
}
