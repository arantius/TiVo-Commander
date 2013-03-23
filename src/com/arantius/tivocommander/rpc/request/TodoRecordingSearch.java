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

import java.util.ArrayList;

import com.arantius.tivocommander.rpc.MindRpc;
import com.fasterxml.jackson.databind.JsonNode;

public class TodoRecordingSearch extends MindRpcRequest {
  public TodoRecordingSearch(ArrayList<JsonNode> showIds, String orderBy) {
    super("recordingSearch");

    mDataMap.put("bodyId", MindRpc.mBodyId);
    mDataMap.put("levelOfDetail", "medium");
    mDataMap.put("objectIdAndType", showIds);
    mDataMap.put("state", new String[] { "inProgress", "scheduled" });
  }
}
