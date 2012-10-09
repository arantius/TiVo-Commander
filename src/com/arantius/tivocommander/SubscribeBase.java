package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.arantius.tivocommander.rpc.request.Subscribe;

abstract public class SubscribeBase extends Activity {
  protected final static String[] mStartLabels = new String[] { "On time",
      "1 minute early", "2 minutes early", "3 minutes early",
      "4 minutes early", "5 minutes early", "10 minutes early" };
  protected final static Integer[] mStartStopValues = new Integer[] { 0,
      60 * 1, 60 * 2, 60 * 3, 60 * 4, 60 * 5, 60 * 10 };
  protected final static String[] mStopLabels = new String[] { "On time",
      "1 minute late", "2 minutes late", "3 minutes late", "4 minutes late",
      "5 minutes late", "10 minutes late" };
  protected final static String[] mUntilLabels = new String[] { "Space needed",
      "Until I delete" };
  protected final static String[] mUntilValues = new String[] { "fifo",
      "forever" };
  protected String mKeepBehavior = null;
  protected String mKeepUntil = null;
  protected Integer mPaddingStart = null;
  protected Integer mPaddingStop = null;

  protected void getValues() {
    int pos;
    pos = ((Spinner) findViewById(R.id.until)).getSelectedItemPosition();
    mKeepUntil = mUntilValues[pos];

    pos = ((Spinner) findViewById(R.id.start)).getSelectedItemPosition();
    mPaddingStart = mStartStopValues[pos];

    pos = ((Spinner) findViewById(R.id.stop)).getSelectedItemPosition();
    mPaddingStop = mStartStopValues[pos];
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
  }

  protected void setUpSpinner(int spinnerId, String[] labels) {
    Spinner spinner = (Spinner) findViewById(spinnerId);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
            labels);
    adapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(adapter);
  }

  protected void subscribeRequestCommon(Subscribe request) {
    request.setKeepUntil(mKeepUntil);
    request.setPadding(mPaddingStart, mPaddingStop);
  }
}
