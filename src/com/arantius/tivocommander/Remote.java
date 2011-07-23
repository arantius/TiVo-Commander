package com.arantius.tivocommander;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.KeyEventSend;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;

public class Remote extends Activity implements OnClickListener {
  public void onClick(View v) {
    String eventStr = null;
    // @formatter:off
    switch (v.getId()) {
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
      MindRpcRequest request = new KeyEventSend(eventStr);
      MindRpc.addRequest(request, null);
      return;
    }

    char eventChar = '\0';
    // @formatter:off
    switch (v.getId()) {
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
      MindRpcRequest request = new KeyEventSend(eventChar);
      MindRpc.addRequest(request, null);
      return;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setTitle("TiVo Commander - Remote Control");
    setContentView(R.layout.remote);
  }

  @Override
  public void onResume() {
    super.onResume();
    MindRpc.init(this);
  }
}
