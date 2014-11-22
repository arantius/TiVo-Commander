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

package com.arantius.tivocommander;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.CollectionSearch;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;
import com.fasterxml.jackson.databind.JsonNode;

abstract public class ExploreCommon extends Activity {
  private final MindRpcResponseListener mListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          if ("error".equals(response.getBody().path("type").asText())) {
            if ("staleData".equals(response.getBody().path("code"))) {
              Utils.toast(ExploreCommon.this, "Stale data error, panicking.",
                  Toast.LENGTH_SHORT);
              finish();
              return;
            }
          }

          JsonNode body = response.getBody();
          if (body.has("collection")) {
            mContent = response.getBody().path("collection").path(0);
          } else if (body.has("recording")) {
            mContent = response.getBody().path("recording").path(0);
          } else if (body.has("content")) {
            mContent = response.getBody().path("content").path(0);
          } else {
            Utils.toast(ExploreCommon.this, "Response missing content",
                Toast.LENGTH_SHORT);
            finish();
            return;
          }
          onContent();
        }
      };

  protected String mCollectionId = null;
  protected JsonNode mContent = null;
  protected String mContentId = null;
  protected String mOfferId = null;
  protected String mRecordingId = null;

  protected MindRpcRequest getRequest() {
    if (mRecordingId != null) {
      return new RecordingSearch(mRecordingId);
    } else if (mContentId != null) {
      return new ContentSearch(mContentId);
    } else if (mCollectionId != null) {
      return new CollectionSearch(mCollectionId);
    } else {
      final String message = "Content: Bad input!";
      Utils.toast(ExploreCommon.this, message, Toast.LENGTH_SHORT);
      Utils.logError(message);
      finish();
      return null;
    }
  }

  abstract protected void onContent();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Bundle bundle = getIntent().getExtras();
    if (MindRpc.init(this, bundle))
      return;

    if (bundle != null) {
      mCollectionId = bundle.getString("collectionId");
      mContentId = bundle.getString("contentId");
      mOfferId = bundle.getString("offerId");
      mRecordingId = bundle.getString("recordingId");
    }

    getParent().setProgressBarIndeterminateVisibility(true);
    MindRpcRequest req = getRequest();
    MindRpc.addRequest(req, mListener);
  }

  protected void setRefreshResult() {
    Intent resultIntent = new Intent();
    resultIntent.putExtra("refresh", true);
    getParent().setResult(Activity.RESULT_OK, resultIntent);
  }
}
