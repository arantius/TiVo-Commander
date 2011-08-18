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

import org.codehaus.jackson.JsonNode;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.arantius.tivocommander.rpc.MindRpc;
import com.arantius.tivocommander.rpc.request.CollectionSearch;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.request.MindRpcRequest;
import com.arantius.tivocommander.rpc.request.RecordingSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

abstract public class ExploreCommon extends Activity {
  private final MindRpcResponseListener mListener =
      new MindRpcResponseListener() {
        public void onResponse(MindRpcResponse response) {
          if ("error".equals(response.getBody().path("type").getValueAsText())) {
            if ("staleData".equals(response.getBody().path("code"))) {
              Toast.makeText(getBaseContext(), "Stale data error, panicing.",
                  Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), "Response missing content",
                Toast.LENGTH_SHORT).show();
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
    if (mContentId != null) {
      return new ContentSearch(mContentId);
    } else if (mCollectionId != null) {
      return new CollectionSearch(mCollectionId);
    } else if (mRecordingId != null) {
      return new RecordingSearch(mRecordingId);
    } else {
      final String message = "Content: Bad input!";
      Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
      Utils.logError(message, null);
      finish();
      return null;
    }
  }

  abstract protected void onContent();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mCollectionId = bundle.getString("collectionId");
      mContentId = bundle.getString("contentId");
      mOfferId = bundle.getString("offerId");
      mRecordingId = bundle.getString("recordingId");
    }

    getParent().setProgressBarIndeterminateVisibility(true);
    MindRpcRequest req = getRequest();
    if (req != null) { // Because of bad input?
      MindRpc.addRequest(req, mListener);
    }
  }
}
