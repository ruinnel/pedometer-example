/*
 * Filename	: AppModule.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dagger.Module;
import dagger.Provides;
import net.ruinnel.pedometer.db.DatabaseManager;

import javax.inject.Singleton;

import static android.content.Context.LOCATION_SERVICE;

@Module
public class AppModule {

  private static final String JSON_DATE_FORMAT = "yyyy-MM-dd kk:mm:ss";

  private final Pedometer application;
  public AppModule(Pedometer nexHome) {
    this.application = nexHome;
  }

  @Provides @Singleton
  Context provideApplicationContext() {
    return this.application.getApplicationContext();
  }

  // Dagger will only look for methods annotated with @Provides
  @Provides
  @Singleton
  // Application reference must come from AppModule.class
  SharedPreferences providesSharedPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  @Provides
  @Singleton
  Settings providesSettings(Context context) {
    return Settings.getInstance(context);
  }

  @Provides
  @Singleton
  Gson providesGsons() {
    return new GsonBuilder().setDateFormat(JSON_DATE_FORMAT).create();
  }

  @Provides
  @Singleton
  LocationManager provideLocationManager() {
    return (LocationManager) application.getSystemService(LOCATION_SERVICE);
  }

  @Provides
  @Singleton
  SensorManager provideSensorManager() {
    return (SensorManager) application.getSystemService(Context.SENSOR_SERVICE);
  }

  @Provides
  @Singleton
  DatabaseManager provideDatabaseManager() {
    return new DatabaseManager(application);
  }
}
