/*
 * Filename	: MiniPedometerService.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.danialgoodwin.globaloverlay.GlobalOverlay;
import com.danialgoodwin.globaloverlay.OnDoubleTapListener;
import net.ruinnel.pedometer.db.DatabaseManager;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.util.Utils;
import net.ruinnel.pedometer.view.MainActivity;

import javax.inject.Inject;

/**
 * Created by ruinnel on 2017. 1. 12..
 */
public class OverlayService extends Service {
  private static final String TAG = OverlayService.class.getSimpleName();

  public class OverlayServiceBinder extends Binder {
    public OverlayService getService() {
      return OverlayService.this;
    }
  }

  private final IBinder mBinder = new OverlayServiceBinder();

  private Pedometer mApp;

  @Inject
  DatabaseManager mDbManager;

  @Inject
  Settings mSettings;

  private GlobalOverlay mGlobalOverlay;
  private View mView;

  private TextView mTxtSteps;
  private TextView mTxtDistance;

  private Toast mToast;

  public void showToast(int strResId) {
    showToast(getString(strResId));
  }

  public void showToast(CharSequence msg) {
    if (mToast != null) {
      mToast.cancel();
    }
    mToast = Toast.makeText(this, msg, Snackbar.LENGTH_INDEFINITE);
    mToast.setDuration(Snackbar.LENGTH_LONG);
    mToast.show();
  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.i(TAG, "onCreate called!");
    mApp = (Pedometer) getApplication();
    mApp.component().inject(this);
    mGlobalOverlay = new GlobalOverlay(this);

    mView = LayoutInflater.from(this).inflate(R.layout.overlay, null);
    mTxtSteps = (TextView) mView.findViewById(R.id.txt_steps);
    mTxtDistance = (TextView) mView.findViewById(R.id.txt_distance);
    mGlobalOverlay.addOverlayView(mView, new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        // on click action...
      }
    }, new OnDoubleTapListener() {
      @Override
      public void onDoubleTap() {
        Intent intent = new Intent(mApp, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        stopSelf(); // Stop service not needed.
      }
    });

    refreshViews();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.i(TAG, "onDestroy called!");
    if (mView != null) {
      mGlobalOverlay.removeOverlayView(mView);
    }
  }

  private void refreshViews() {
    // steps couter
    History history = mDbManager.todayHistory();
    int steps = (history != null ? history.steps : 0);
    mTxtSteps.setText(String.valueOf(steps));

    // distance
    int strides = mSettings.getStrides();
    double distance = Utils.getDistance(steps, strides);
    mTxtDistance.setText(String.format(getString(R.string.format_distance), distance));
  }
}
