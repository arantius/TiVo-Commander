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

import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class Utils {
  private static boolean DEBUG = false;
  private static final String LOG_TAG = "tivo_commander";
  private static final ObjectMapper mMapper = new ObjectMapper();
  private static final ObjectWriter mMapperPretty = mMapper
      .defaultPrettyPrintingWriter();

  public final static void debugLog(String message) {
    if (DEBUG) {
      log(message);
    }
  }

  public static final String findImageUrl(JsonNode node) {
    String url = null;
    int biggestSize = 0;
    int size = 0;
    for (JsonNode image : node.path("image")) {
      size =
          image.path("width").getIntValue()
              * image.path("height").getIntValue();
      if (size > biggestSize) {
        biggestSize = size;
        url = image.path("imageUrl").getTextValue();
      }
    }
    return url;
  }

  public final static String getVersion(Context context) {
    String version = " v";
    try {
      PackageManager pm = context.getPackageManager();
      version += pm.getPackageInfo(context.getPackageName(), 0).versionName;
    } catch (NameNotFoundException e) {
      version = "";
    }
    return version;
  }

  public static final String join(String glue, List<String> strings) {
    Iterator<String> it = strings.iterator();
    StringBuilder out = new StringBuilder();
    String s;
    while (it.hasNext()) {
      s = it.next();
      if (s == null || s == "") {
        continue;
      }
      out.append(s);
      if (it.hasNext()) {
        out.append(glue);
      }
    }

    return out.toString();
  }

  public static final String join(String glue, String... strings) {
    return join(glue, Arrays.asList(strings));
  }

  public final static void log(String message) {
    Log.i(LOG_TAG, message);
  }

  public final static void logError(String message) {
    Log.e(LOG_TAG, message);
  }

  public final static void logError(String message, Throwable e) {
    Log.e(LOG_TAG, message, e);
  }

  public final static void logRpc(Object obj) {
    if (DEBUG) {
      Log.d(LOG_TAG, Utils.stringifyToPrettyJson(obj));
    }
  }

  public static final void mailLog(String log, Context context, String title) {
    Intent i = new Intent(Intent.ACTION_SEND);
    i.setType("message/rfc822");
    i.putExtra(Intent.EXTRA_EMAIL, new String[] { "arantius+tivo@gmail.com" });
    i.putExtra(Intent.EXTRA_SUBJECT, "DVR Commander for TiVo " + title);
    i.putExtra(Intent.EXTRA_TEXT, "Please describe you were doing when "
        + "something went wrong:\n\n\n\nThen leave these details for me:\n"
        + "Version: " + getVersion(context) + "\n" + log);
    try {
      context.startActivity(Intent.createChooser(i, "Send mail..."));
    } catch (android.content.ActivityNotFoundException ex) {
      Toast.makeText(context, "There are no email clients installed.",
          Toast.LENGTH_SHORT).show();
    }
  }

  @TargetApi(11)
  public final static void activateHomeButton(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      ActionBar ab = activity.getActionBar();
      ab.setDisplayHomeAsUpEnabled(true);
    }
  }

  public final static boolean onCreateOptionsMenu(Menu menu, Activity activity) {
    Utils.activateHomeButton(activity);
    // TODO: Not show the current activity.
    MenuInflater inflater = activity.getMenuInflater();
    inflater.inflate(R.menu.main, menu);
    return true;
  }

  public final static boolean onOptionsItemSelected(MenuItem item,
      Activity activity) {
    Intent intent = null;
    switch (item.getItemId()) {
    case android.R.id.home:
      intent = new Intent(activity, Catalog.class);
      break;
    case R.id.menu_item_remote:
      intent = new Intent(activity, Remote.class);
    case R.id.menu_item_my_shows:
      intent = new Intent(activity, MyShows.class);
      break;
    case R.id.menu_item_search:
      intent = new Intent(activity, Search.class);
      break;
    case R.id.menu_item_settings:
      intent = new Intent(activity, Discover.class);
      break;
    case R.id.menu_item_help:
      intent = new Intent(activity, Help.class);
      break;
    case R.id.menu_item_about:
      intent = new Intent(activity, About.class);
      break;
    default:
      Utils.logError("Unknown item ID: " + Integer.toString(item.getItemId()));
      return false;
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    activity.startActivity(intent);
    return true;
  }

  public final static Date parseDateStr(String dateStr) {
    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd");
    return parseDateTimeStr(dateParser, dateStr);
  }

  public final static Date parseDateTimeStr(String dateStr) {
    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    return parseDateTimeStr(dateParser, dateStr);
  }

  public final static JsonNode parseJson(String json) {
    try {
      return mMapper.readValue(json, JsonNode.class);
    } catch (JsonMappingException e) {
      Log.e(LOG_TAG, "parseJson failure", e);
    } catch (JsonParseException e) {
      Log.e(LOG_TAG, "parseJson failure", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "parseJson failure", e);
    }
    logError("When parsing:\n" + json);
    return null;
  }

  public final static String stringifyToJson(Object obj) {
    return stringifyToJson(obj, false);
  }

  public final static String stringifyToPrettyJson(Object obj) {
    return stringifyToJson(obj, true);
  }

  public final static String ucFirst(String s) {
    if (s == null) {
      return null;
    }
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  private final static Date parseDateTimeStr(SimpleDateFormat dateParser,
      String dateStr) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    dateParser.setTimeZone(tz);
    ParsePosition pp = new ParsePosition(0);
    return dateParser.parse(dateStr, pp);
  }

  private final static String stringifyToJson(Object obj, boolean pretty) {
    try {
      if (pretty) {
        return mMapperPretty.writeValueAsString(obj);
      } else {
        return mMapper.writeValueAsString(obj);
      }
    } catch (JsonGenerationException e) {
      Log.e(LOG_TAG, "stringifyToJson failure", e);
    } catch (JsonMappingException e) {
      Log.e(LOG_TAG, "stringifyToJson failure", e);
    } catch (IOException e) {
      Log.e(LOG_TAG, "stringifyToJson failure", e);
    }
    return null;
  }
}
