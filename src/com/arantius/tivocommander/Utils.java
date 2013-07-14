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
import java.util.Locale;
import java.util.TimeZone;

import android.annotation.SuppressLint;
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
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Utils {
  public static boolean DEBUG_LOG = false;

  private static final String LOG_TAG = "tivo_commander";
  private static final ObjectMapper mMapper = new ObjectMapper();
  private static final ObjectWriter mMapperPretty = mMapper
      .writerWithDefaultPrettyPrinter();

  @TargetApi(11)
  public final static void activateHomeButton(Activity activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      ActionBar ab = activity.getActionBar();
      ab.setDisplayHomeAsUpEnabled(true);
    }
  }

  private final static Class<? extends Activity> activityForMenuId(int menuId) {
    switch (menuId) {
    case android.R.id.home:
      return NowShowing.class;
    case R.id.menu_item_remote:
      return Remote.class;
    case R.id.menu_item_my_shows:
      return MyShows.class;
    case R.id.menu_item_search:
      return Search.class;
    case R.id.menu_item_todo:
      return ToDo.class;
    case R.id.menu_item_season_pass:
      return SeasonPass.class;
    case R.id.menu_item_settings:
      return Discover.class;
    case R.id.menu_item_help:
      return Help.class;
    case R.id.menu_item_about:
      return About.class;
    }
    return null;
  }

  @SuppressLint("NewApi")
  final static void addToMenu(Menu menu, Activity activity, int itemId,
      int iconId, String title, int showAsAction) {
    if (Utils.activityForMenuId(itemId) == activity.getClass()) {
      return;
    }
    MenuItem menuitem = menu.add(Menu.NONE, itemId, Menu.NONE, title);
    menuitem.setIcon(iconId);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      menuitem.setShowAsAction(showAsAction);
    }
  }

  @SuppressLint("InlinedApi")
  public final static void createFullOptionsMenu(Menu menu, Activity activity) {
    Utils.activateHomeButton(activity);

    addToMenu(menu, activity, R.id.menu_item_remote, R.drawable.icon_remote,
        "Remote", MenuItem.SHOW_AS_ACTION_IF_ROOM);
    addToMenu(menu, activity, R.id.menu_item_my_shows, R.drawable.icon_tv32,
        "My Shows", MenuItem.SHOW_AS_ACTION_IF_ROOM);
    addToMenu(menu, activity, R.id.menu_item_search, R.drawable.icon_search,
        "Search", MenuItem.SHOW_AS_ACTION_IF_ROOM);
    addToMenu(menu, activity, R.id.menu_item_todo, R.drawable.icon_todo,
        "To Do List", MenuItem.SHOW_AS_ACTION_NEVER);
    addToMenu(menu, activity, R.id.menu_item_season_pass,
        R.drawable.icon_seasonpass,
        "Season Pass Manager", MenuItem.SHOW_AS_ACTION_NEVER);
    addToMenu(menu, activity, R.id.menu_item_settings, R.drawable.icon_cog,
        "Settings", MenuItem.SHOW_AS_ACTION_NEVER);
    addToMenu(menu, activity, R.id.menu_item_help, R.drawable.icon_help,
        "Help", MenuItem.SHOW_AS_ACTION_NEVER);
    addToMenu(menu, activity, R.id.menu_item_about, R.drawable.icon_info,
        "About", MenuItem.SHOW_AS_ACTION_NEVER);
  }

  @SuppressLint("InlinedApi")
  public final static void createHelpOptionsMenu(Menu menu, Activity activity) {
    addToMenu(menu, activity, R.id.menu_item_help, R.drawable.icon_help,
        "Help", MenuItem.SHOW_AS_ACTION_NEVER);
    addToMenu(menu, activity, R.id.menu_item_about, R.drawable.icon_info,
        "About", MenuItem.SHOW_AS_ACTION_NEVER);
  }

  public static final String findImageUrl(JsonNode node) {
    String url = null;
    int biggestSize = 0;
    int size = 0;
    for (JsonNode image : node.path("image")) {
      size = image.path("width").asInt() * image.path("height").asInt();
      if (size > biggestSize) {
        biggestSize = size;
        url = image.path("imageUrl").asText();
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

  public final static void logDebug(String message) {
    if (DEBUG_LOG) Log.d(LOG_TAG, message);
  }

  public final static void logError(String message) {
    Log.e(LOG_TAG, message);
  }

  public final static void logError(String message, Throwable e) {
    Log.e(LOG_TAG, message, e);
  }

  public final static void logRpc(Object obj) {
    String json = Utils.stringifyToPrettyJson(obj);
    for (String line : json.split(System.getProperty("line.separator"))) {
      Utils.logDebug(line);
    }
  }

  public final static boolean onOptionsItemSelected(MenuItem item,
      Activity srcActivity) {
    return onOptionsItemSelected(item, srcActivity, false);
  }

  public final static boolean onOptionsItemSelected(MenuItem item,
      Activity srcActivity, boolean homeIsBack) {
    if (android.R.id.home == item.getItemId() && homeIsBack) {
      srcActivity.finish();
      return true;
    }

    Class<? extends Activity> targetActivity =
        Utils.activityForMenuId(item.getItemId());
    if (targetActivity == null) {
      Utils.logError("Unknown menu item ID: "
          + Integer.toString(item.getItemId()));
      return false;
    }
    Intent intent = new Intent(srcActivity, targetActivity);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    srcActivity.startActivity(intent);
    return true;
  }

  public final static Date parseDateStr(String dateStr) {
    SimpleDateFormat dateParser = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    return parseDateTimeStr(dateParser, dateStr);
  }

  private final static Date parseDateTimeStr(SimpleDateFormat dateParser,
      String dateStr) {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    dateParser.setTimeZone(tz);
    ParsePosition pp = new ParsePosition(0);
    return dateParser.parse(dateStr, pp);
  }

  public final static Date parseDateTimeStr(String dateStr) {
    SimpleDateFormat dateParser =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
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

  public final static String stringifyToPrettyJson(Object obj) {
    return stringifyToJson(obj, true);
  }

  public final static String stripQuotes(String s) {
    if (s.length() <= 1) return s;
    if ('"' == s.charAt(0) && '"' == s.charAt(s.length() - 1)) {
      return s.substring(1, s.length() - 1);
    }
    return s;
  }

  public final static SubscriptionType subscriptionTypeForRecording(
      JsonNode recording) {
    if ("inProgress".equals(recording.path("state").asText())) {
      return SubscriptionType.RECORDING;
    }
    String subType = recording.path("subscriptionIdentifier").path(0)
        .path("subscriptionType").asText();
    if ("seasonPass".equals(subType)) {
      return SubscriptionType.SEASON_PASS;
    } else if ("singleOffer".equals(subType)) {
      return SubscriptionType.SINGLE_OFFER;
    } else if ("wishList".equals(subType)) {
      return SubscriptionType.WISHLIST;
    } else {
      logError("Unsupported subscriptionType string: " + subType);
      return null;
    }
  }

  public final static void toast(Activity activity, int messageId, int length) {
    Context ctx = activity.getBaseContext();
    Toast.makeText(ctx, messageId, length).show();
  }

  public final static void toast(Activity activity, String message, int length) {
    Context ctx = activity.getBaseContext();
    Toast.makeText(ctx, message, length).show();
  }

  public final static String ucFirst(String s) {
    if (s == null) {
      return null;
    }
    return s.substring(0, 1).toUpperCase(Locale.US) + s.substring(1);
  }
}
