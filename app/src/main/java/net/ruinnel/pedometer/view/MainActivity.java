/*
 * Filename	: MainActivity.java
 * Comment 	:
 * History	: 2017/01/11, ruinnel, Create
 *
 * Version	: 1.0
 * Author   : Copyright (c) 2017 by ruinnel. All Rights Reserved.
 */

package net.ruinnel.pedometer.view;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ToggleButton;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import net.ruinnel.pedometer.BuildConfig;
import net.ruinnel.pedometer.NetModule;
import net.ruinnel.pedometer.R;
import net.ruinnel.pedometer.Settings;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.view.adapter.ViewPagerAdapter;
import net.ruinnel.pedometer.view.widget.BaseActivity;
import okhttp3.Request;
import okio.Buffer;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class MainActivity extends BaseActivity
  implements NetModule.RequestModifiedListener,
  GoogleApiClient.ConnectionCallbacks,
  GoogleApiClient.OnConnectionFailedListener,
  ViewPager.OnPageChangeListener,
  LocationListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  public static final int INTERVAL = 10 * 60 * 1000;      // 10분
  public static final int DISPLACEMENT = 100;              // 100m
  public static final int USER_PERMISSION_REQUEST = 9876;

  private static final int MSG_LOCATION_TIMEOUT = 1;
  private static final int LOCATION_TIMEOUT = 10 * 1000; // 10 sec

  // view pager index
  private static final int IDX_MAIN = 0;
  private static final int IDX_HISTORY = 1;

  @Inject
  Settings mSettings;

  @Inject
  SensorManager mSensorManager;

  @Inject
  Gson mGson;

  @BindView(R.id.main_viewpager)
  ViewPager mPager;

  @BindView(R.id.tab_main)
  ToggleButton mBtnMain;

  @BindView(R.id.tab_history)
  ToggleButton mBtnHistory;

  private ViewPagerAdapter mAdapter;

  private GoogleApiClient mGoogleApiClient;

  private Location mLastLocation;

  private Handler mHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApp.component().inject(this);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    if (BuildConfig.DEBUG) {
      mApp.setRequestModifiedListener(this);
    }

    mPager = (ViewPager) findViewById(R.id.main_viewpager);
    mAdapter = new ViewPagerAdapter(this, getSupportFragmentManager());
    mPager.setAdapter(mAdapter);
    mPager.addOnPageChangeListener(this);
    refreshTab(IDX_MAIN);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mPager != null) {
      mPager.removeOnPageChangeListener(this);
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (checkPlayServices() && checkPermission()) {
      connectGoogleApiClient();
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    if (mGoogleApiClient != null) {
      stopLocationUpdates();
      mGoogleApiClient.disconnect();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  public boolean checkPermission() {
    boolean permitted = false;
    boolean needDialog = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        permitted = false;
        Log.d(TAG, "permitted = " + permitted);

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, USER_PERMISSION_REQUEST);
        } else {
          needDialog = true;
        }

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, USER_PERMISSION_REQUEST);
        } else {
          needDialog = true;
        }

        if (needDialog) {
          showLocationPermissionDialog();
        }
      } else {
        permitted = true;
      }
    } else {
      permitted = true;
    }

    return permitted;
  }

  private void showLocationPermissionDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
      .setTitle(R.string.notification)
      .setMessage(R.string.need_location_permission)
      .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          startInstalledAppDetailsActivity();
        }
      }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        showSnackbar(R.string.need_location_permission);
      }
    }).show();
  }

  @Override
  public void requestModified(Request request) {
    // TODO for debug
    Log.d(TAG, "request = " + request.toString());
    final StringBuffer buf = new StringBuffer();
    buf.append(String.format("%s %s %s\n", request.method(), request.url(), "HTTP/1.1"));
    if (request.headers() != null) {
      Set<String> names = request.headers().names();
      Iterator<String> itr = names.iterator();
      while (itr.hasNext()) {
        String name = itr.next();
        String val = request.header(name);
        buf.append(String.format("%s: %s\n", name, val));
      }
    }

    buf.append("\n");
    if (request.body() != null) {
      //buf.append(request.toString());
      Buffer buffer = new Buffer();
      try {
        request.body().writeTo(buffer);
        buf.append(buffer.readString(request.body().contentLength(), Charset.forName("UTF-8")));
      } catch (Exception e) {
      }
    }
    Log.d(TAG, buf.toString());
  }

  private synchronized void connectGoogleApiClient() {
    Log.d(TAG, "connectGoogleApiClient");
    mGoogleApiClient = new GoogleApiClient.Builder(this)
      .addConnectionCallbacks(this)
      .addOnConnectionFailedListener(this)
      .addApi(LocationServices.API).build();
    mGoogleApiClient.connect();
  }

  /**
   * Method to verify google play services on the device
   */
  private boolean checkPlayServices() {
    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
    int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      // TODO 에러처리 필요.
      showSnackbar(R.string.google_play_service_not_found);
      //      if (apiAvailability.isUserResolvableError(resultCode)) {
      //        apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
      //      } else {
      //        Log.i(TAG, "This device is not supported.");
      //      }
      //      Log.i(TAG, "This device is not supported.");
      return false;
    }
    return true;
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

    if (mLastLocation == null) {
      Log.i(TAG, "Couldn't get the location. Make sure location is enabled on the device.");
    } else {
      Log.i(TAG, "LastLocation " + mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude() + " (" + mLastLocation.getProvider() + ")");
    }

    startLocationUpdates();
    // current location timeout...
    mHandler = new Handler(new Handler.Callback() {
      @Override
      public boolean handleMessage(Message message) {
        if (message.what == MSG_LOCATION_TIMEOUT) {
          if (mLastLocation != null) {
            Intent broadcast = new Intent(Settings.ACTION_FIND_LOCATION);
            broadcast.putExtra(Settings.EXTRA_LOCATION, mLastLocation);
            LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(broadcast);
          } else {
            // TODO
            showSnackbar(R.string.location_not_found);
          }
        }
        return false;
      }
    });
    mHandler.sendEmptyMessageDelayed(MSG_LOCATION_TIMEOUT, LOCATION_TIMEOUT);
  }

  @Override
  public void onConnectionSuspended(int i) {
    // reconnect..
    mGoogleApiClient.connect();
  }


  /**
   * Google api callback methods
   */
  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    mLastLocation = null;
  }

  /**
   * Starting the location updates
   */
  protected void startLocationUpdates() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
      && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return;
    }

    Log.i(TAG, "startLocationUpdates");

    LocationRequest request = new LocationRequest();
    request.setInterval(INTERVAL);
    request.setFastestInterval(INTERVAL);
    request.setSmallestDisplacement(DISPLACEMENT);
    request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
  }

  /**
   * Stopping location updates
   */
  protected void stopLocationUpdates() {
    Log.i(TAG, "stopLocationUpdates");
    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
  }

  @Override
  public void onLocationChanged(Location location) {
    Intent broadcast = new Intent(Settings.ACTION_FIND_LOCATION);
    broadcast.putExtra(Settings.EXTRA_LOCATION, location);
    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (grantResults != null && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      connectGoogleApiClient();
    } else {
      showSnackbar(R.string.need_location_permission);
    }
  }

  private void startInstalledAppDetailsActivity() {
    final Intent intent = new Intent();
    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    this.startActivity(intent);
  }

  @Override
  public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

  }

  @Override
  public void onPageSelected(int position) {
    refreshTab(position);
  }

  @Override
  public void onPageScrollStateChanged(int state) {}

  private void refreshTab(int position) {
    mBtnMain.setChecked(false);
    mBtnHistory.setChecked(false);
    if (position == IDX_MAIN) {
      mBtnMain.setChecked(true);
    } else {
      mBtnHistory.setChecked(true);
    }
  }

  @OnClick(R.id.tab_main)
  public void onClickMainTab(View view) {
    mPager.setCurrentItem(IDX_MAIN);
    refreshTab(IDX_MAIN);
  }

  @OnClick(R.id.tab_history)
  public void onClickHistoryTab(View view) {
    mPager.setCurrentItem(IDX_HISTORY);
    refreshTab(IDX_HISTORY);
  }
}