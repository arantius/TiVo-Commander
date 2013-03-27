package com.arantius.tivocommander;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
}
