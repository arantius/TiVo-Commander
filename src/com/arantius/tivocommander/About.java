package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.arantius.tivocommander.rpc.MindRpc;

public class About extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

    TextView title = (TextView) findViewById(R.id.textView1);
    title.setText(title.getText() + Utils.getVersion(this));
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:About");
    MindRpc.init(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:About");
  }
}
