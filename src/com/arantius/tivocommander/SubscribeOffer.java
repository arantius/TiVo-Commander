package com.arantius.tivocommander;

import android.os.Bundle;
import android.view.View;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.Subscribe;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class SubscribeOffer extends SubscribeBase {
  public void doSubscribe(View v) {
    getValues();

    Subscribe request = new Subscribe();
    Bundle bundle = getIntent().getExtras();
    request
        .setOffer(bundle.getString("offerId"), bundle.getString("contentId"));
    subscribeRequestCommon(request);

    Utils.showProgress(this, true);
    MindRpc.addRequest(request, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        finish();
      }
    });
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this, getIntent().getExtras());

    setContentView(R.layout.subscribe_offer);
    setUpSpinner(R.id.until, mUntilLabels);
    setUpSpinner(R.id.start, mStartLabels);
    setUpSpinner(R.id.stop, mStopLabels);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:SubscribeOffer");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:SubscribeOffer");
    MindRpc.init(this, getIntent().getExtras());
  }
}
