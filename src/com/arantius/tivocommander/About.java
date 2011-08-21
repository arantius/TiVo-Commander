package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

    TextView title = (TextView) findViewById(R.id.textView1);
    title.setText(title.getText() + Utils.getVersion(this));
  }
}
