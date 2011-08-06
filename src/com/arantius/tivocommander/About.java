package com.arantius.tivocommander;

import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.widget.TextView;

public class About extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.about);

    String version = " v";
    try {
      version +=
          getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (NameNotFoundException e) {
      version = "";
    }

    TextView title = (TextView) findViewById(R.id.textView1);
    title.setText(title.getText() + version);
  }
}
