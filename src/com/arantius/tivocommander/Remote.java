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
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.KeyEventSend;

public class Remote extends Activity implements OnClickListener {
  private EditText mEditText;
  private InputMethodManager mInputManager;

  private final TextWatcher mTextWatcher = new TextWatcher() {
    public void afterTextChanged(Editable s) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count,
        int after) {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (count == 0) {
        return;
      } else if (before > count) {
        while (before > count) {
          MindRpc.addRequest(viewIdToEvent(R.id.remote_reverse), null);
          count++;
        }
      } else {
        for (int i = start + before; i < start + count; i++) {
          MindRpc.addRequest(new KeyEventSend(s.charAt(i)), null);
        }
      }
    }
  };

  public void onClick(View v) {
    if (v.getId() == R.id.remote_clear) {
      mEditText.setText("");
    }

    MindRpc.addRequest(viewIdToEvent(v.getId()), null);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    setTitle("TiVo Commander - Remote Control");
    setContentView(R.layout.remote);

    mEditText = (EditText) findViewById(R.id.keyboard_activator);
    mEditText.addTextChangedListener(mTextWatcher);
    mInputManager =
        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

    // TODO: Do not open keyboard on launch.
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }

  public void toggleKeyboard(View v) {
    Utils.log("clicked keyboard");

    mEditText.setText("");
    mEditText.requestFocus();
    mInputManager.toggleSoftInputFromWindow(mEditText.getWindowToken(), 0, 0);
  }

  private KeyEventSend viewIdToEvent(int id) {
    String eventStr = null;
    // @formatter:off
    switch (id) {
      case R.id.remote_tivo:        eventStr = "tivo"; break;
      case R.id.remote_liveTv:      eventStr = "liveTv"; break;
      case R.id.remote_info:        eventStr = "info"; break;
      case R.id.remote_zoom:        eventStr = "zoom"; break;
      case R.id.remote_guide:       eventStr = "guide"; break;
      case R.id.remote_up:          eventStr = "up"; break;
      case R.id.remote_down:        eventStr = "down"; break;
      case R.id.remote_left:        eventStr = "left"; break;
      case R.id.remote_right:       eventStr = "right"; break;
      case R.id.remote_select:      eventStr = "select"; break;
      case R.id.remote_channelUp:   eventStr = "channelUp"; break;
      case R.id.remote_channelDown: eventStr = "channelDown"; break;
      case R.id.remote_thumbsDown:  eventStr = "thumbsDown"; break;
      case R.id.remote_thumbsUp:    eventStr = "thumbsUp"; break;
      case R.id.remote_record:      eventStr = "record"; break;
      case R.id.remote_play:        eventStr = "play"; break;
      case R.id.remote_pause:       eventStr = "pause"; break;
      case R.id.remote_reverse:     eventStr = "reverse"; break;
      case R.id.remote_forward:     eventStr = "forward"; break;
      case R.id.remote_slow:        eventStr = "slow"; break;
      case R.id.remote_replay:      eventStr = "replay"; break;
      case R.id.remote_advance:     eventStr = "advance"; break;
      case R.id.remote_actionA:     eventStr = "actionA"; break;
      case R.id.remote_actionB:     eventStr = "actionB"; break;
      case R.id.remote_actionC:     eventStr = "actionC"; break;
      case R.id.remote_actionD:     eventStr = "actionD"; break;
      case R.id.remote_clear:       eventStr = "clear"; break;
      case R.id.remote_enter:       eventStr = "enter"; break;
    }
    // @formatter:on
    if (eventStr != null) {
      return new KeyEventSend(eventStr);
    }

    char eventChar = '\0';
    // @formatter:off
    switch (id) {
      case R.id.remote_num1:        eventChar = '1'; break;
      case R.id.remote_num2:        eventChar = '2'; break;
      case R.id.remote_num3:        eventChar = '3'; break;
      case R.id.remote_num4:        eventChar = '4'; break;
      case R.id.remote_num5:        eventChar = '5'; break;
      case R.id.remote_num6:        eventChar = '6'; break;
      case R.id.remote_num7:        eventChar = '7'; break;
      case R.id.remote_num8:        eventChar = '8'; break;
      case R.id.remote_num9:        eventChar = '9'; break;
      case R.id.remote_num0:        eventChar = '0'; break;
    }
    // @formatter:on
    if (eventChar != '\0') {
      return new KeyEventSend(eventChar);
    }

    return null;
  }
}
