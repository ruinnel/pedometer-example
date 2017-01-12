/*
 * Filename	: StepListener.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.util;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public interface StepListener {
  void onStep();

  void onStepCount(int steps);
}
