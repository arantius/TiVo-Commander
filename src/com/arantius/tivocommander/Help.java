package com.arantius.tivocommander;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Help extends Activity {
  public final void customSettings(View v) {
    Intent intent = new Intent(this, Settings.class);
    startActivity(intent);
    finish();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.help);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      ((TextView) findViewById(R.id.note)).setText(bundle.getString("note"));
      findViewById(R.id.note).setVisibility(View.VISIBLE);
    }

    Utils.activateHomeButton(this);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return Utils.onOptionsItemSelected(item, this, true);
  }

  @Override
  protected void onPause() {
    super.onPause();
    Utils.log("Activity:Pause:Help");
  }

  @Override
  protected void onResume() {
    super.onResume();
    Utils.log("Activity:Resume:Help");
  }

  public final void sendReport(View v) {
    String buildProp = "unknown";
    try {
      InputStream propStream = Runtime.getRuntime().exec("/system/bin/getprop")
          .getInputStream();

      StringBuffer buildPropBuf = new StringBuffer("");
      byte[] buffer = new byte[1024];
      while (propStream.read(buffer) != -1) {
        buildPropBuf.append(new String(buffer));
      }
      buildProp = buildPropBuf.toString();
    } catch (IOException e) {
      // Ignore.
      buildProp = "Error:\n" + e.toString();
    }

    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("message/rfc822");
    i.putExtra(Intent.EXTRA_EMAIL, new String[] { "arantius+tivo@gmail.com" });
    i.putExtra(Intent.EXTRA_SUBJECT, "Error Log -- DVR Commander for TiVo");
    i.putExtra(Intent.EXTRA_TEXT,
        "Log data for the developer:\n\n"
        + "Version: " + Utils.getVersion(this) + "\n\n"
        + "Raw logs:\n" + Utils.logBufferAsString() + "\n\n"
        + "build.prop:\n" + buildProp);

    try {
      this.startActivity(Intent.createChooser(i, "Send mail..."));
    } catch (android.content.ActivityNotFoundException ex) {
      Utils.toast(this, "There are no email clients installed.",
          Toast.LENGTH_SHORT);
    }

    finish();
  }
}
