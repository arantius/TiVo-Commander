package com.arantius.tivocommander;

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.CollectionSearch;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class ExploreCommon extends Activity {
  private final MindRpcResponseListener mCollectionListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mContent = response.getBody().path("collection").path(0);
          onContent();
        }
      };
  private final MindRpcResponseListener mContentListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          if ("error".equals(response.getBody().path("type").getValueAsText())) {
            if ("staleData".equals(response.getBody().path("code"))) {
              Toast.makeText(getBaseContext(), "Stale data error, panicing.",
                  Toast.LENGTH_SHORT).show();
              finish();
              return;
            }
          }

          mContent = response.getBody().path("content").path(0);
          onContent();
        }
      };
  protected String mCollectionId = null;
  protected JsonNode mContent = null;
  protected String mContentId = null;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mContentId = bundle.getString("contentId");
      mCollectionId = bundle.getString("collectionId");
    }

    setContentView(R.layout.progress);
    if (mContentId != null) {
      MindRpc.addRequest(new ContentSearch(mContentId), mContentListener);
    } else if (mCollectionId != null) {
      MindRpc.addRequest(new CollectionSearch(mCollectionId),
          mCollectionListener);
    } else {
      final String message = "Content: Bad input!";
      Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
      Utils.logError(message, null);
      finish();
    }
  }

  protected final String findImageUrl(JsonNode node) {
    String url = null;
    int biggestSize = 0;
    int size = 0;
    for (JsonNode image : node.path("image")) {
      size =
          image.path("width").getIntValue()
              * image.path("height").getIntValue();
      if (size > biggestSize) {
        biggestSize = size;
        url = image.path("imageUrl").getTextValue();
      }
    }
    return url;
  }

  protected void onContent() {
    // Should be implemented by child -- how do I represent that properly?
  }
}
