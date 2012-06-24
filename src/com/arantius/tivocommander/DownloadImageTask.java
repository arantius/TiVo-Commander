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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;

public class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
  private final static long MAX_CACHE_SIZE = 2 * 1024 * 1024; // 2Mb

  private final Context mContext;
  private final ImageView mImageView;
  private final View mProgressView;
  private final ResponseCache mResponseCache = new ResponseCache() {
    // Thanks to: http://pivotallabs.com/users/tyler/blog/articles/1754
    @Override
    public CacheResponse get(URI uri, String rqstMethod,
        Map<String, List<String>> rqstHeaders) throws IOException {
      final File file =
          new File(mContext.getCacheDir(), java.net.URLEncoder.encode(
              uri.toString(), "UTF-8"));
      if (file.exists()) {
        return new CacheResponse() {
          @Override
          public Map<String, List<String>> getHeaders() throws IOException {
            return null;
          }

          @Override
          public InputStream getBody() throws IOException {
            file.setLastModified(System.currentTimeMillis());
            return new FileInputStream(file);
          }
        };
      } else {
        return null;
      }
    }

    @Override
    public CacheRequest put(URI uri, URLConnection conn) throws IOException {
      final File file =
          new File(mContext.getCacheDir(), java.net.URLEncoder.encode(conn
              .getURL().toString(), "UTF-8"));
      return new CacheRequest() {
        @Override
        public OutputStream getBody() throws IOException {
          // Maintain a maximum cache size; 5% garbage collect.
          if (new Random().nextDouble() >= 0.95) {
            File[] files = mContext.getCacheDir().listFiles();

            Arrays.sort(files, new Comparator<File>() {
              public int compare(File f1, File f2) {
                return Long.valueOf(f1.lastModified()).compareTo(
                    f2.lastModified());
              }
            });

            long cacheSize = 0;
            for (File file : files) {
              cacheSize += file.length();
              if (cacheSize > MAX_CACHE_SIZE) {
                file.delete();
              }
            }
          }

          return new FileOutputStream(file);
        }

        @Override
        public void abort() {
          file.delete();
        }
      };
    }
  };

  public DownloadImageTask(Context context, ImageView imageView,
      View progressView) {
    mContext = context;
    mImageView = imageView;
    mProgressView = progressView;
    ResponseCache.setDefault(mResponseCache);
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
      conn.setUseCaches(true);
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
