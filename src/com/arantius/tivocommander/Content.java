package com.arantius.tivocommander;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.request.RecordingUpdate;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Content extends Activity {
  private final class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
    @Override
    protected Bitmap doInBackground(String... urls) {
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
        mImageView.setBackgroundDrawable(new BitmapDrawable(result));
      }
      mImageProgress.setVisibility(View.INVISIBLE);
    }
  }

  private final MindRpcResponseListener contentListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          setContentView(R.layout.content);

          mContent = response.getBody().path("content").path(0);
          mImageView = findViewById(R.id.content_layout_image);
          mImageProgress = findViewById(R.id.content_image_progress);
          mRecordingId =
              mContent.path("recordingForContentId").path(0)
                  .path("recordingId").getTextValue();

          // Display titles.
          String title = mContent.path("title").getTextValue();
          String subtitle = mContent.path("subtitle").getTextValue();
          ((TextView) findViewById(R.id.content_title)).setText(title);
          TextView subtitleView =
              ((TextView) findViewById(R.id.content_subtitle));
          if (subtitle == null) {
            setTitle(title);
            subtitleView.setHeight(0);
          } else {
            setTitle(title + " - " + subtitle);
            subtitleView.setText(subtitle);
          }

          // Construct and display details.
          ArrayList<String> detailParts = new ArrayList<String>();
          int season = mContent.path("seasonNumber").getIntValue();
          int epNum = mContent.path("episodeNum").path(0).getIntValue();
          if (season != 0 && epNum != 0) {
            detailParts.add(String.format("Sea %d Ep %d", season, epNum));
          }
          detailParts.add(mContent.path("tvRating").getTextValue());
          detailParts.add(mContent.path("category").path(0).path("label")
              .getTextValue());
          int year = mContent.path("originalAirYear").getIntValue();
          if (year != 0) {
            detailParts.add(Integer.toString(year));
          }
          String detail1 = "(" + Utils.joinList(", ", detailParts) + ")";
          String detail2 = mContent.path("description").getTextValue();
          TextView detailView = ((TextView) findViewById(R.id.content_details));
          if (detail2 == null) {
            detailView.setText(detail1);
          } else {
            Spannable details = new SpannableString(detail1 + " " + detail2);
            details.setSpan(new ForegroundColorSpan(Color.WHITE),
                detail1.length(), details.length(), 0);
            detailView.setText(details);
          }
          // Find and set the image if possible.
          boolean foundImage = false;
          JsonNode images = mContent.path("image");
          for (JsonNode image : images) {
            if ("showcaseBanner".equals(image.path("imageType").getTextValue())
                && 200 == image.path("width").getIntValue()
                && 150 == image.path("height").getIntValue()) {
              new DownloadImageTask().execute(image.path("imageUrl")
                  .getTextValue());
              foundImage = true;
              break;
            }
          }
          if (!foundImage) {
            mImageProgress.setVisibility(View.INVISIBLE);
          }
        }
      };

  private JsonNode mContent;
  private String mContentId;
  private View mImageView;
  private View mImageProgress;
  private String mRecordingId;

  public void doDelete(View v) {
    MindRpc.addRequest(new RecordingUpdate(mRecordingId, "deleted"), null);
    finish();
  }

  public void doExplore(View v) {
    Toast.makeText(getBaseContext(), "Explore not implemented yet.",
        Toast.LENGTH_SHORT).show();
  }

  public void doWatch(View v) {
    MindRpc.addRequest(new UiNavigate(mRecordingId), null);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mContentId = bundle.getString("com.arantius.tivocommander.contentId");
    } else {
      Toast.makeText(getBaseContext(), R.string.error_reading_content_id,
          Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    setContentView(R.layout.progress);
    MindRpc.addRequest(new ContentSearch(mContentId), contentListener);
  }
}
