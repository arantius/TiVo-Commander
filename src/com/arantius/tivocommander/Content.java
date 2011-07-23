package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class Content extends Activity {
  private String mContentId;

  private final MindRpcResponseListener contentListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          // todo
        }
      };
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
