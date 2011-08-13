package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.Subscribe;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class SubscribeBase extends Activity {
  protected final static String[] mKeepBehaviors = new String[] { "fifo",
      "duration", "duration", "duration", "duration", "duration", "duration",
      "duration", "forever" };
  protected final static Integer[] mKeepDurations = new Integer[] { null,
      86400 * 1, 86400 * 2, 86400 * 3, 86400 * 4, 86400 * 5, 86400 * 6,
      86400 * 7, null };
  protected final static String[] mKeepLabels = new String[] { "Space needed",
      "1 day", "2 day", "3 day", "4 day", "5 day", "6 day", "7 day",
      "Until I delete" };
  protected final static String[] mStartLabels = new String[] { "On time",
      "1 minute early", "2 minutes early", "3 minutes early",
      "4 minutes early", "5 minutes early", "10 minutes early" };
  protected final static Integer[] mStartStopValues = new Integer[] { null,
      60 * 1, 60 * 2, 60 * 3, 60 * 4, 60 * 5, 60 * 10 };
  protected final static String[] mStopLabels = new String[] { "On time",
      "1 minute late", "2 minutes late", "3 minutes late", "4 minutes late",
      "5 minutes late", "10 minutes late" };

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
    int keepPos =
        ((Spinner) findViewById(R.id.duration)).getSelectedItemPosition();
    request.setKeep(mKeepBehaviors[keepPos], mKeepDurations[keepPos]);

    int startPos =
        ((Spinner) findViewById(R.id.start)).getSelectedItemPosition();
    int stopPos = ((Spinner) findViewById(R.id.stop)).getSelectedItemPosition();
    request.setPadding(mStartStopValues[startPos], mStartStopValues[stopPos]);

    setProgressBarIndeterminateVisibility(true);
    MindRpc.addRequest(request, new MindRpcResponseListener() {
      public void onResponse(MindRpcResponse response) {
        finish();
      }
    });
  }
}
