/*
 * Filename	: Settings.java
 * Function	:
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

public class Settings {
  //private static final String TAG = Settings.class.getSimpleName();

  public static final String BASE_URL = "https://openapi.naver.com";
  public static final String CLIENT_ID = "7ZBRhuQ6ufaL7ZjNDP8Q";
  public static final String CLIENT_SECRET = "JDt6XV4OHR";

  public static final String ACTION_STEP = "net.ruinnel.pedometer.ACTION_STEP";
  public static final String ACTION_FIND_LOCATION = "net.ruinnel.pedometer.ACTION_FIND_LOCATION";
  public static final String ACTION_STRIDES_CHANGED = "net.ruinnel.pedometer.ACTION_STRIDES_CHANGED";
  public static final String EXTRA_LOCATION = "location";

  // 보폭 default(남: 66, 여: 61) -> 중간값 사용
  private static final int STRIDE_DEFAULT = 63;

  // keys
  private static final String KEY_IS_STARTED = "is_started";
  private static final String KEY_PAUSE_STEPS = "pause_steps";
  private static final String KEY_STRIDES = "strides";
  private static final String KEY_IS_STRIDES_SETTED = "is_strides_setted";
  private static final String KEY_IS_USE_OVERLAY = "is_use_overlay";
  private static final String KEY_IS_USE_LOCATION = "is_use_location";

  private final Context mContext;
  private static Settings mInstance;
  private final SharedPreferences mPref;

  public static Settings getInstance(Context context) {
    if (mInstance == null) {
      mInstance = new Settings(context);
    }
    return mInstance;
  }

  public String getNaverBaseURL() {
    return BASE_URL;
  }

  public String getNaverClientId() {
    return CLIENT_ID;
  }

  public String getNaverClientSecret() {
    return CLIENT_SECRET;
  }

  public String getUserAgent() {
    StringBuilder result = new StringBuilder(64);
    result.append("Dalvik/");
    result.append(System.getProperty("java.vm.version")); // such as 1.1.0
    result.append(" (Linux; U; Android ");

    String version = Build.VERSION.RELEASE; // "1.0" or "3.4b5"
    result.append(version.length() > 0 ? version : "1.0");

    // add the model for the release build
    if ("REL".equals(Build.VERSION.CODENAME)) {
      String model = Build.MODEL;
      if (model.length() > 0) {
        result.append("; ");
        result.append(model);
      }
    }
    String id = Build.ID; // "MASTER" or "M4-rc20"
    if (id.length() > 0) {
      result.append(" Build/");
      result.append(id);
    }

    String manufacturer = Build.MANUFACTURER;
    String brand = Build.BRAND;
    if (manufacturer.length() > 0) {
      result.append("/");
      result.append(manufacturer);
    } else if (brand.length() > 0) {
      result.append("/");
      result.append(brand);
    }

    result.append(")");
    return result.toString();
  }

  private Settings(Context context) {
    mContext = context;
    mPref = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public void setStarted(boolean started) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putBoolean(KEY_IS_STARTED, started);
    editor.commit();
  }

  public boolean isStarted() {
    return mPref.getBoolean(KEY_IS_STARTED, false);
  }

  public void setPauseSteps(int steps) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putInt(KEY_PAUSE_STEPS, steps);
    editor.commit();
  }

  public int getPauseSteps() {
    return mPref.getInt(KEY_PAUSE_STEPS, 0);
  }

  public void setStrides(int strides) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putInt(KEY_STRIDES, strides);
    editor.commit();
  }

  public int getStrides() {
    return mPref.getInt(KEY_STRIDES, STRIDE_DEFAULT);
  }

  public void setStridesSetted(boolean setted) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putBoolean(KEY_IS_STRIDES_SETTED, setted);
    editor.commit();
  }

  public boolean isStridesSetted() {
    return mPref.getBoolean(KEY_IS_STRIDES_SETTED, false);
  }

  public void setUseOverlay(boolean use) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putBoolean(KEY_IS_USE_OVERLAY, use);
    editor.commit();
  }

  public boolean isUseOverlay() {
    return mPref.getBoolean(KEY_IS_USE_OVERLAY, true);
  }

  public void setUseLocation(boolean use) {
    SharedPreferences.Editor editor = mPref.edit();
    editor.putBoolean(KEY_IS_USE_LOCATION, use);
    editor.commit();
  }

  public boolean isUseLocation() {
    return mPref.getBoolean(KEY_IS_USE_LOCATION, true);
  }
}
