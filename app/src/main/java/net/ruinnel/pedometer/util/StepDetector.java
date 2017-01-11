/*
 * Filename	: StepDetector.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Queue;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class StepDetector implements SensorEventListener {
  private static final String TAG = StepDetector.class.getSimpleName();

  private static final int TERM = 100; // 100ms
  private static final double THRESHOLD_MIN = 2.0;
  private static final double THRESHOLD_MAX = 10.0;

  private long mLastUpdate;

  private List<StepListener> mListeners;

  private Queue<Double> mQueue;

  public StepDetector() {
    mListeners = Lists.newArrayList();
    mQueue = Lists.newLinkedList();
  }

  public void addStepListener(StepListener listener) {
    if (!mListeners.contains(listener)) {
      mListeners.add(listener);
    }
  }

  public void removeStepListener(StepListener listener) {
    mListeners.remove(listener);
  }

  private void sendToListener() {
    for (StepListener listener : mListeners) {
      listener.onStep();
    }
  }

  private void sendToListener(int steps) {
    for (StepListener listener : mListeners) {
      listener.onStepCount(steps);
    }
  }

  @Override
  public void onSensorChanged(SensorEvent event) {
    Sensor sensor = event.sensor;
    if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
      int steps = (int) event.values[0];
      if (steps > 0) {
        sendToListener(steps);
      }
    } else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      long current = System.currentTimeMillis();
      long diff = current - mLastUpdate;
      if (diff > TERM) {
        mLastUpdate = current;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // 참조 : http://www.jksii.or.kr/upload/1/845_1.pdf
        double svm = Math.sqrt((x * x) + (y * y) + (z * z));
        //Log.i(TAG, "svm = " + svm);

        mQueue.offer(svm);
        if (mQueue.size() > 20) { // 누적 데이터가 2 sec 이상.
          while (mQueue.size() >= 10) {
            Double peak = mQueue.poll();
            Double bottom = mQueue.peek();
            if (peak > bottom) { // 그래프가 하강 곡선일 경우에만...
              mQueue.poll(); // 현재 bottom값을 Queue에서 제거
              while (mQueue.size() > 10) {
                Double val = mQueue.peek();
                if (val < bottom) { // 그래프 하강 구간의 최저값을 구함.
                  bottom = mQueue.poll();
                } else { // 상승 곡선으로 변하면. break
                  break;
                }
              }

              double bandwidth = peak - bottom;
              //Log.d(TAG, "peak = " + peak + ", bottom = " + bottom + ", bandwidth = " + bandwidth);
              if (THRESHOLD_MIN < bandwidth && bandwidth < THRESHOLD_MAX) {
                sendToListener();
              }
            }
          }
        }
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int i) {
    // skip
  }
}
