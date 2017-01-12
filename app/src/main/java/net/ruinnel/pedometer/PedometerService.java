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
import net.ruinnel.pedometer.db.DatabaseManager;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.util.StepDetector;
import net.ruinnel.pedometer.util.StepListener;
import net.ruinnel.pedometer.util.Utils;

import javax.inject.Inject;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class PedometerService extends Service implements StepListener {
  private static final String TAG = PedometerService.class.getSimpleName();

  //private static final int STEP_COUNTER_LATENCY = 1 * 60 * 1000 * 1000; // 1 min // microsecond
  private static final int STEP_COUNTER_LATENCY = 10 * 1000 * 1000; // 10 sec // microsecond

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

  @Inject
  DatabaseManager mDbManager;

  private StepDetector mStepDetector;
  private boolean mIsRegistered;

  @Override
  public void onCreate() {
    super.onCreate();
    mIsRegistered = false;
    mApp = (Pedometer) getApplication();
    mApp.component().inject(this);
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.v(TAG, "onStartCommand called!");
    mStepDetector = new StepDetector();
    mStepDetector.addStepListener(this);
    // 종료 후
    Log.v(TAG, "isStarted = " + mSettings.isStarted());
    if (mSettings.isStarted()) {
      reRegisterSensorListener();
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.v(TAG, "onDestroy called!");
    super.onDestroy();
    mSensorManager.unregisterListener(mStepDetector);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  public void startPedometer() {
    mSettings.setStarted(true);
    reRegisterSensorListener();
  }

  public void stopPedometer() {
    mSettings.setStarted(false);
    unregisterSensorListener();
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

    Log.d(TAG, "onStep - 1");
    History history = mDbManager.todayHistory();
    if (history == null) {
      history = new History();
      history.day = Utils.getToday();
      history.steps = 1;
      mDbManager.saveHistory(history);
    } else {
      history.steps = history.steps + 1;
      mDbManager.updateHistory(history);
    }
  }

  @Override
  public void onStepCount(int steps) {
    Log.d(TAG, "onStepCount = " + steps);

    int pauseSteps = mSettings.getPauseSteps();
    if (pauseSteps == 0) { // init
      mSettings.setPauseSteps(steps);
      pauseSteps = steps;
    }

    int diff = steps - pauseSteps;
    if (diff > 0) {
      History history = mDbManager.todayHistory();
      if (history == null) {
        history = new History();
        history.day = Utils.getToday();
        history.steps = diff;
        mDbManager.saveHistory(history);
      } else {
        history.steps = history.steps + diff;
        mDbManager.updateHistory(history);
      }

      Log.d(TAG, "paused = " + pauseSteps + ", today = " + history.steps + ", diff = " + diff);

      mSettings.setPauseSteps(steps);
    }

    Intent broadcast = new Intent(Settings.ACTION_STEP);
    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
  }
}
