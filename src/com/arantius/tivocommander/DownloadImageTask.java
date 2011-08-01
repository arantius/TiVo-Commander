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
