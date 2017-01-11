/*
 * Filename	: PedometerService.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.util.StepDetector;
import net.ruinnel.pedometer.util.StepListener;

import javax.inject.Inject;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class PedometerService extends Service implements StepListener {
  private static final String TAG = PedometerService.class.getSimpleName();

  //private static final int STEP_COUNTER_LATENCY = 5 * 60 * 1000 * 1000; // 5 min // microsecond
  private static final int STEP_COUNTER_LATENCY = 5 * 1000 * 1000; // 5 sec // microsecond

  public class PedometerServiceBinder extends Binder {
    public PedometerService getService() {
      return PedometerService.this;
    }
  }

  private final IBinder mBinder = new PedometerServiceBinder();

  private Pedometer mApp;

  @Inject
  Settings mSettings;

  @Inject
  SensorManager mSensorManager;

  private StepDetector mStepDetector;
  private boolean mIsRegistered;

  @Override
  public void onCreate() {
    Log.i(TAG, "onCreate called!");
    super.onCreate();
    mIsRegistered = false;
    mApp = (Pedometer) getApplication();
    mApp.component().inject(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.i(TAG, "onStartCommand called!");
    mStepDetector = new StepDetector();
    mStepDetector.addStepListener(this);
    reRegisterSensorListener();
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "onDestroy called!");
    super.onDestroy();
    mSensorManager.unregisterListener(mStepDetector);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  private void reRegisterSensorListener() {
    Log.d(TAG, "re-register sensor listener");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) { // over KITKAT_WATCH(20)
      Sensor stepCounter = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
      if (stepCounter != null) {
        Log.i(TAG, "using TYPE_STEP_COUNTER");
        unregisterSensorListener();
        mSensorManager.registerListener(mStepDetector, stepCounter, SensorManager.SENSOR_DELAY_NORMAL, STEP_COUNTER_LATENCY);
        mIsRegistered = true;
      }
    } else {
      Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
      if (accelerometer != null && !mIsRegistered) {
        Log.i(TAG, "using ACCELEROMETER");
        mSensorManager.registerListener(mStepDetector, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mIsRegistered = true;
      }
    }
  }

  private void unregisterSensorListener() {
    try {
      mIsRegistered = false;
      mSensorManager.unregisterListener(mStepDetector);
      Log.v(TAG, "unregisterSensorListener - " + mStepDetector);
    } catch (Exception e) {
      Log.w(TAG, "unregister sensor lister fail.", e);
    }
  }

  @Override
  public void onStep() {
    Intent broadcast = new Intent(Settings.ACTION_STEP);
    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

    Log.i(TAG, "onStep - 1");
    int todaySteps = mSettings.getTodaySteps();
    mSettings.setTodaySteps(++todaySteps);
  }

  @Override
  public void onStepCount(int steps) {
    Log.i(TAG, "onStepCount = " + steps);

    int pauseSteps = mSettings.getPauseSteps();
    if (pauseSteps == 0) { // init
      mSettings.setPauseSteps(steps);
      pauseSteps = steps;
    }

    int diff = steps - pauseSteps;
    if (diff > 0) {
      int todayTotal = mSettings.getTodaySteps();

      Log.d(TAG, "paused = " + pauseSteps + ", today = " + todayTotal + ", diff = " + diff);
      todayTotal = todayTotal + diff;
      mSettings.setTodaySteps(todayTotal);
      mSettings.setPauseSteps(steps);
    }

    Intent broadcast = new Intent(Settings.ACTION_STEP);
    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);

    //reRegisterSensorListener();
  }
}
