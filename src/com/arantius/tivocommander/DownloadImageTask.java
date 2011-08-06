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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
  private final ImageView mImageView;
  private final View mProgressView;

  public DownloadImageTask(ImageView imageView, View progressView) {
    mImageView = imageView;
    mProgressView = progressView;
  }

  @Override
  protected Bitmap doInBackground(String... urls) {
    if (urls[0] == null) {
      return null;
    }

    URL url = null;
    try {
      url = new URL(urls[0]);
    } catch (MalformedURLException e) {
      Utils.logError("Parse URL; " + urls[0], e);
      return null;
    }

    try {
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setDoInput(true);
      conn.connect();
      InputStream is = conn.getInputStream();
      return BitmapFactory.decodeStream(is);
    } catch (IOException e) {
      Utils.logError("Download URL; " + urls[0], e);
      return null;
    }
  }

  @Override
  protected void onPostExecute(Bitmap result) {
    if (result != null) {
      mImageView.setImageDrawable(new BitmapDrawable(result));
    }
    if (mProgressView != null) {
      mProgressView.setVisibility(View.GONE);
    }
  }
}
