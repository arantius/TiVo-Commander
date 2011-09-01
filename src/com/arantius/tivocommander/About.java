/*
TiVo Commander allows control of a TiVo Premiere device.
Copyright (C) 2011  Anthony Lieuallen (arantius@gmail.com)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

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
