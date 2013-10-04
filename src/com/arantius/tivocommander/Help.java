package com.arantius.tivocommander;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class Help extends Activity {
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

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle("Help me help you!");
    builder.setMessage(
        "Sorry you're having trouble.  But I'm just one guy, giving this app "
        + "away for free.  Please help me help you: describe in detail "
        + "exactly what you did, and be prepared to answer my followup "
        + "questions.");

    // TODO: Be DRY vs. the same in customDevice().
    final OnClickListener onClickListener =
        new OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            sendReport();
          }
        };
    builder.setPositiveButton("OK", onClickListener);
    builder.setNegativeButton("Cancel", null);

    builder.create().show();
  }

  @SuppressLint("WorldReadableFiles")
  void sendReport() {
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
    i.putExtra(Intent.EXTRA_TEXT, "Please explain the problem here:\n\n");

    final String error_text =
        "Log data for the developer:\n\n"
            + "Version: " + Utils.getVersion(this) + "\n\n"
            + "Raw logs:\n" + Utils.logBufferAsString() + "\n\n"
            + "build.prop:\n" + buildProp;

    final String error_file_name = "error.txt";
    try {
      @SuppressWarnings("deprecation")
      FileOutputStream outs = openFileOutput(
          error_file_name, MODE_WORLD_READABLE);
      outs.write(error_text.getBytes(Charset.forName("UTF-8")));
      outs.close();
      // http://stackoverflow.com/a/11955326/91238
      String sdCard =
          Environment.getExternalStorageDirectory().getAbsolutePath();
      Uri uri = Uri.fromFile(new File(sdCard +
          new String(new char[sdCard.replaceAll("[^/]", "").length()])
              .replace("\0", "/..") + getFilesDir() + "/" + error_file_name));
      i.putExtra(android.content.Intent.EXTRA_STREAM, uri);
    } catch (IOException e) {
      Utils.logError("could not write error text", e);
      i.putExtra(
          Intent.EXTRA_TEXT,
          error_text + "\n\nWrite error:\n" + e.toString()
          );
    }

    try {
      this.startActivity(Intent.createChooser(i, "Send mail..."));
    } catch (android.content.ActivityNotFoundException ex) {
      Utils.toast(this, "There are no email clients installed.",
          Toast.LENGTH_SHORT);
    }

    finish();
  }
}
