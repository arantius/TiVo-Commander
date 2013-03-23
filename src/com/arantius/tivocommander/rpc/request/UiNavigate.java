/*
DVR Commander for TiVo allows control of a TiVo Premiere device.
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

package com.arantius.tivocommander.rpc.request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UiNavigate extends MindRpcRequest {
  public UiNavigate(String recordingId) {
    super("uiNavigate");

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("fUseTrioId", "true");
    parameters.put("fHideBannerOnEnter", "false");
    parameters.put("recordingId", recordingId);

    mDataMap.put("uri", "x-tivo:classicui:playback");
    mDataMap.put("parameters", parameters);
  }

  public UiNavigate(List<String> recordingIds, String displayName) {
    super("uiNavigate");

    Map<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("fUseTrioId", "true");
    parameters.put("fHideBannerOnEnter", "false");
    parameters.put("iCurrentRecording", 0);
    parameters.put("recordingId", recordingIds);

    if (displayName != null && !"".equals(displayName)) {
      parameters.put("folderName", displayName);
    }

    mDataMap.put("uri", "x-tivo:classicui:playback");
    mDataMap.put("parameters", parameters);
  }
}
