package com.arantius.tivocommander;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;

import android.util.Log;

public class Utils {
  private static final boolean DEBUG = false;
  private static final String LOG_TAG = "tivo_commander";
  private static final ObjectMapper mMapper = new ObjectMapper();
  private static final ObjectWriter mMapperPretty = mMapper
      .defaultPrettyPrintingWriter();

  public final static void debugLog(String message) {
    if (DEBUG) {
      Log.d(LOG_TAG, message);
    }
  }

  public final static void log(String message) {
    Log.i(LOG_TAG, message);
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
    Log.e(LOG_TAG, "When parsing:\n" + json);
    return null;
  }

  public final static String stringifyToJson(Object obj) {
    return stringifyToJson(obj, false);
  }

  public final static String stringifyToJson(Object obj, boolean pretty) {
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
}
