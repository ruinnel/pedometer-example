/*
 * Filename	: MainFragment.java
 * Comment 	:
 * History	: 2017/01/12, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.ruinnel.pedometer.PedometerService;
import net.ruinnel.pedometer.R;
import net.ruinnel.pedometer.Settings;
import net.ruinnel.pedometer.api.NaverMapClient;
import net.ruinnel.pedometer.api.bean.Error;
import net.ruinnel.pedometer.api.bean.ReverseGeocode;
import net.ruinnel.pedometer.db.DatabaseManager;
import net.ruinnel.pedometer.db.bean.History;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.util.Utils;
import net.ruinnel.pedometer.view.widget.BaseFragment;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by ruinnel on 2017. 1. 12..
 */
public class MainFragment extends BaseFragment {
  private static final String TAG = MainFragment.class.getSimpleName();

  @Inject
  Settings mSettings;

  @Inject
  DatabaseManager mDbManager;

  @Inject
  NaverMapClient mAppClient;

  @BindView(R.id.txt_steps)
  TextView mTxtSteps;

  @BindView(R.id.txt_address)
  TextView mTxtAddress;

  @BindView(R.id.txt_distance)
  TextView mTxtDistance;

  @BindView(R.id.btn_toggle)
  ToggleButton mBtnToggle;

  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (Settings.ACTION_STEP.equals(action)) {
        refreshViews();
      } else if (Settings.ACTION_FIND_LOCATION.equals(action)) {
        Location location = intent.getParcelableExtra(Settings.EXTRA_LOCATION);
        requestReverseGeocode(location.getLatitude(), location.getLongitude());
      }
    }
  };

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    if (mView == null) {
      mView = inflater.inflate(R.layout.frag_main, container, false);
    } else {
      removeView();
    }

    mApp.component().inject(this);
    ButterKnife.bind(this, mView);

    refreshViews();

    //TODO 테스트 데이터 생성 - 하루전 부터. 100일치 데이터 생성
    //makeTestData();

    return mView;
  }

  // TODO 나중에 지울것
  private void makeTestData() {
    SimpleDateFormat formater = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
    Calendar cal = Calendar.getInstance();
    cal.setTime(Utils.getToday());
    SecureRandom random = new SecureRandom();
    Log.d(TAG, "today - " + formater.format(cal.getTime()));
    for (int i = 0; i < 100; i++) {
      cal.add(Calendar.DAY_OF_MONTH, -1);
      Log.d(TAG, "day = " + formater.format(cal.getTime()));
      int steps = Math.abs(random.nextInt() % 10000);
      History history = new History();
      history.day = cal.getTime();
      history.steps = steps;
      mDbManager.saveHistory(history);
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    if (!getUserVisibleHint()) {
      return;
    }

    // TODO fragment가 보여질때 실행할 로직.
    registerReceiver();
  }

  @Override
  public void setUserVisibleHint(boolean isVisibleToUser) {
    super.setUserVisibleHint(isVisibleToUser);
    if (isVisibleToUser && isResumed()) {
      onResume();
    }
  }

  @Override
  public void onPause() {
    super.onPause();

  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    unregisterReceiver();
  }

  private void registerReceiver() {
    unregisterReceiver();
    IntentFilter filter = new IntentFilter(Settings.ACTION_STEP);
    filter.addAction(Settings.ACTION_FIND_LOCATION);
    LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
  }

  private void unregisterReceiver() {
    try {
      LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
    } catch (Exception e) {
      Log.w(TAG, "unregisterReceiver fail.");
    }
  }

  @OnClick(R.id.btn_toggle)
  public void onToggle(View view) {
    PedometerService service = mApp.pedometerService();
    if (service != null) {
      if (!mSettings.isStarted()) {
        service.startPedometer();
      } else {
        service.stopPedometer();
      }
    }

    refreshViews();
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

    // button
    mBtnToggle.setChecked(mSettings.isStarted());
  }

  private void requestReverseGeocode(double lat, double lng) {
    Call<ReverseGeocode> call = mAppClient.reverseGeocode(String.format("%f,%f", lng, lat));
    call.enqueue(new Callback<ReverseGeocode>() {
      @Override
      public void onResponse(Call<ReverseGeocode> call, Response<ReverseGeocode> response) {
        if (response.isSuccessful()) {
          ReverseGeocode.Item item = response.body().result.getFirstItem();
          if (item != null) {
            mTxtAddress.setText(item.address);
          } else {
            mTxtAddress.setText(R.string.current_location_unknown);
          }
        } else {
          Error error = parseErrorBody(response.errorBody());
          showSnackbar(error.errorMessage);
        }
      }

      @Override
      public void onFailure(Call<ReverseGeocode> call, Throwable t) {
        showSnackbar(R.string.msg_network_error);
      }
    });
  }

  public Error parseErrorBody(ResponseBody errorBody) {
    try {
      Gson gson = new Gson();
      Type type = new TypeToken<Error>() {}.getType();
      return gson.fromJson(errorBody.string(), type);
    } catch (Exception e) {
      Log.w(TAG, "parseErrorBody fail !! ", e);
      return null;
    }
  }
}
