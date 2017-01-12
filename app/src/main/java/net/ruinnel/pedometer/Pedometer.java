/*
 * Filename	: Pedometer.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import dagger.Component;
import net.ruinnel.pedometer.util.Utils;
import net.ruinnel.pedometer.view.MainActivity;
import net.ruinnel.pedometer.view.fragment.HistoryFragment;
import net.ruinnel.pedometer.view.fragment.MainFragment;
import net.ruinnel.pedometer.view.widget.BaseActivity;
import net.ruinnel.pedometer.view.widget.BaseFragment;
import okhttp3.Request;

import javax.inject.Singleton;

/**
 * Created by ruinnel on 2017. 1. 10..
 */
public class Pedometer extends Application {
  private static final String TAG = Pedometer.class.getSimpleName();

  @Singleton
  @Component(modules = {AppModule.class, NetModule.class})
  public interface ApplicationComponent {
    // application
    void inject(Pedometer application);

    // activity
    void inject(BaseActivity baseActivity);

    void inject(MainActivity mainActivity);

    // fragment
    void inject(BaseFragment baseFragment);

    void inject(MainFragment mainFragment);

    void inject(HistoryFragment historyFragment);

    // service
    void inject(PedometerService pedometerService);
  }

  private boolean mIsPedometerServiceBounding;
  private Intent mServiceIntent;
  private ServiceConnection mPedometerServiceConnection;
  private PedometerService mPedometerService;

  private ApplicationComponent component;

  public ApplicationComponent component() {
    return component;
  }

  // Okhttp3의 interceptor로 Request가 변경될 경우 callback.(디버깅용)
  private NetModule.RequestModifiedListener mRequestModifiedListener;

  public void setRequestModifiedListener(NetModule.RequestModifiedListener listener) {
    mRequestModifiedListener = listener;
  }

  @Override
  public void onCreate() {
    super.onCreate();

    component = DaggerPedometer_ApplicationComponent.builder()
      .appModule(new AppModule(this))
      .netModule(new NetModule(new NetModule.RequestModifiedListener() {
        @Override
        public void requestModified(Request request) {
          if (mRequestModifiedListener != null) {
            mRequestModifiedListener.requestModified(request);
          }
        }
      }))
      .build();
    component().inject(this); // As of now, LocationManager should be injected into this.

    mIsPedometerServiceBounding = false;
    startPedometerService();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
  }

  public PedometerService pedometerService() {
    return mPedometerService;
  }

  public void startPedometerService() {
    if (mIsPedometerServiceBounding) {
      return;
    }

    mPedometerServiceConnection = new ServiceConnection() {
      public void onServiceConnected(ComponentName className, IBinder service) {
        mPedometerService = ((PedometerService.PedometerServiceBinder) service).getService();
      }

      public void onServiceDisconnected(ComponentName className) {}
    };

    mServiceIntent = new Intent(this, PedometerService.class);
    if (!Utils.isServiceRunning(getApplicationContext(), PedometerService.class)) {
      startService(mServiceIntent);
    }

    mIsPedometerServiceBounding = true;
    bindService(mServiceIntent, mPedometerServiceConnection, Context.BIND_AUTO_CREATE);
  }

  public void stopPedometerService() {
    if (mIsPedometerServiceBounding && mPedometerServiceConnection != null) {
      mIsPedometerServiceBounding = false;
      unbindService(mPedometerServiceConnection);
      stopService(mServiceIntent);
    }
  }
}
