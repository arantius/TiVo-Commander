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
import com.arantius.tivocommander.rpc.request.BaseSearch;
import com.arantius.tivocommander.rpc.request.CollectionSearch;
import com.arantius.tivocommander.rpc.request.ContentSearch;
import com.arantius.tivocommander.rpc.response.MindRpcResponse;
import com.arantius.tivocommander.rpc.response.MindRpcResponseListener;

public class ExploreCommon extends Activity {
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
          } else if (body.has("content")) {
            mContent = response.getBody().path("content").path(0);
          } else {
            Toast.makeText(getApplicationContext(),
                "Response missing collection and content", Toast.LENGTH_SHORT)
                .show();
            finish();
            return;
          }
          onContent();
        }
      };
  protected String mCollectionId = null;
  protected JsonNode mContent = null;
  protected String mContentId = null;

  protected BaseSearch getRequest() {
    if (mContentId != null) {
      return new ContentSearch(mContentId);
    } else if (mCollectionId != null) {
      return new CollectionSearch(mCollectionId);
    } else {
      final String message = "Content: Bad input!";
      Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
      Utils.logError(message, null);
      finish();
      return null;
    }
  }

  protected void onContent() {
    // TODO: Should be implemented by child -- how do I represent that properly?
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    MindRpc.init(this);

    Bundle bundle = getIntent().getExtras();
    if (bundle != null) {
      mContentId = bundle.getString("contentId");
      mCollectionId = bundle.getString("collectionId");
    }

    setContentView(R.layout.progress);
    BaseSearch req = getRequest();
    MindRpc.addRequest(req, mListener);
  }
}
