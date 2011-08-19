package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class ProblemReport extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.problem_report);
  }

  public void sendReport(View v) {
    Utils.mailLog(Utils.getLog(), this, "Problem Report");
  }
}
