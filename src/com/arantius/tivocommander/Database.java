package com.arantius.tivocommander;

import java.util.ArrayList;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

public class Database extends SQLiteOpenHelper {
  private static final String DATABASE_NAME = "dvr_commander";
  private static final int DATABASE_VERSION = 1;
  private static final String QUERY_SELECT_DEVICE =
      "SELECT id, addr, device_name, mak, port, tsn FROM devices";

  public Database(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  private Device deviceFromCursor(Cursor cursor) {
    Device device = new Device();
    device.id = Long.parseLong(cursor.getString(0));
    device.addr = cursor.getString(1);
    device.device_name = cursor.getString(2);
    device.mak = cursor.getString(3);
    device.port = Integer.parseInt(cursor.getString(4));
    device.tsn = cursor.getString(5);
    return device;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE devices ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "used_time INTEGER, "
            + "addr TEXT, "
            + "device_name TEXT, "
            + "mak TEXT, "
            + "port INTEGER, "
            + "tsn TEXT"
            + ");"
        );
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // TODO
  }

  public void deleteDevice(Long id) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete("devices", "id = ?", new String[] { String.valueOf(id) });
    db.close();
  }

  public Device getDevice(final Long id) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        QUERY_SELECT_DEVICE + " WHERE id = ?",
        new String[] { id.toString() });

    Device device = null;
    if (cursor.moveToFirst()) {
      device = deviceFromCursor(cursor);
    }
    db.close();
    return device;
  }

  public ArrayList<Device> getDevices() {
    ArrayList<Device> devices = new ArrayList<Device>();

    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        QUERY_SELECT_DEVICE + " ORDER BY device_name ASC",
        null);

    if (cursor.moveToFirst()) {
      do {
        devices.add(deviceFromCursor(cursor));
      } while (cursor.moveToNext());
    }
    db.close();

    return devices;
  }

  public Device getLastDevice() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        QUERY_SELECT_DEVICE + " ORDER BY used_time DESC LIMIT 1",
        null);

    Device device = null;
    if (cursor.moveToFirst()) {
      device = deviceFromCursor(cursor);
    }
    db.close();

    return device;
  }

  public Device getNamedDevice(String name, String addr) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cursor = db.rawQuery(
        QUERY_SELECT_DEVICE + " WHERE device_name = ? AND addr = ?"
        + " ORDER BY used_time DESC LIMIT 1",
        new String[] { name, addr });

    Device device = null;
    if (cursor.moveToFirst()) {
      device = deviceFromCursor(cursor);
    }
    db.close();

    return device;
  }

  public void portLegacySettings(Context context) {
    final SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(context);

    if ("missing".equals(prefs.getString("tivo_addr", "missing"))) {
      return;
    }

    Device device = new Device();
    device.device_name = "Unknown";
    device.addr = prefs.getString("tivo_addr", "");
    device.port = Integer.parseInt(prefs.getString("tivo_port", "0"));
    device.mak = prefs.getString("tivo_mak", "");

    this.saveDevice(device);

    // TODO: remove prefs
  }

  public void saveDevice(Device device) {
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put("addr", device.addr);
    values.put("device_name", device.device_name);
    values.put("mak", device.mak);
    values.put("port", device.port);
    values.put("tsn", device.tsn);

    if (device.id == null) {
      long id = db.insertOrThrow("devices", null, values);
      device.id = id;
    } else {
      db.update(
          "devices", values,
          "id = ?", new String[] { String.valueOf(device.id) });
    }

    db.close();
  }

  public void switchDevice(Device device) {
    SQLiteDatabase db = this.getWritableDatabase();

    ContentValues values = new ContentValues();
    values.put("used_time", System.currentTimeMillis());

    db.update(
        "devices", values,
        "id = ?", new String[] { String.valueOf(device.id) });
    db.close();

    Utils.log(String.format(
        Locale.US,
        "Switched to device: %s / %s / %d",
        device.addr, device.device_name, device.id
        ));
  }
}
