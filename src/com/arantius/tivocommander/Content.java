package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.request.UiNavigate;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Content extends Activity {
  private final MindRpcResponseListener contentListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          mResponse = response;
          setContentView(R.layout.content);
        }
      };

  private String mContentId;
  private MindRpcResponse mResponse;

  public void doDelete(View v) {
    Toast.makeText(getBaseContext(), "Delete not implemented yet.",
        Toast.LENGTH_SHORT).show();
  }

  public void doExplore(View v) {
    Toast.makeText(getBaseContext(), "Explore not implemented yet.",
        Toast.LENGTH_SHORT).show();
  }

  public void doWatch(View v) {
    String recordingId =
        mResponse.getBody().path("content").path(0)
            .path("recordingForContentId").path(0).path("recordingId")
            .getTextValue();
    MindRpc.addRequest(new UiNavigate(recordingId), null);
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
