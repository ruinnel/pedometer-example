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
import dagger.Component;
import net.ruinnel.pedometer.view.MainActivity;
import net.ruinnel.pedometer.view.widget.BaseActivity;
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

    // service
    void inject(PedometerService pedometerService);
  }

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
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
  }

  @Override
  public void onTrimMemory(int level) {
    super.onTrimMemory(level);
  }

}
