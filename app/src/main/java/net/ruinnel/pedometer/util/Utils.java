/*
 * Filename	: Utils.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class Utils {
  public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  public static Date getToday() {
    return getDay(new Date());
  }

  public static Date getDay(Date date) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(date);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  public static double getDistance(int steps, int strides) {
    double distance = (steps * (strides / 100.0) / 1000.0);
    if (distance < 0.1) { // 0.0km 방지.
      distance = 0.1;
    }
    return distance;
  }
}
