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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import java.util.Arrays;
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
  public static final int REQ_LOCATION_PERMISSION = 9876;
  public static final int REQ_OVERLAY_PERMISSION = REQ_LOCATION_PERMISSION + 1;

  private static final int MSG_LOCATION_TIMEOUT = 1;
  private static final int LOCATION_TIMEOUT = 10 * 1000; // 10 sec

  // view pager index
  private static final int IDX_MAIN = 0;
  private static final int IDX_HISTORY = 1;

  @Inject
  Settings mSettings;

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

  private AlertDialog mDialog;

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
    // check location permission & start location service
    if (checkPlayServices()) {
      if (mSettings.isUseLocation()) {
        if (checkLocationPermission()) {
          connectGoogleApiClient();
        }
      }
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
    mApp.stopOverlayService();

    // check overlay permission
    // onStart에서 처리시 위치권한 다어얼로그와 충돌
    if (mSettings.isUseOverlay()) {
      checkOverlayPermission();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mSettings.isStarted()) {
      if (mSettings.isUseOverlay()) {
        mApp.startOverlayService();
      } else {
        showSnackbar(R.string.please_enable_overlay);
      }
    }
  }

  private boolean checkOverlayPermission() {
    boolean permitted = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      permitted = android.provider.Settings.canDrawOverlays(this);
      Log.i(TAG, "canDrawOverlays = " + permitted);
      if (!permitted) {
        showOverlayPermissionDialog();
      }
    } else {
      permitted = true;
    }

    return permitted;
  }

  private boolean checkLocationPermission() {
    boolean permitted = false;
    boolean needDialog = false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        permitted = false;

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION_PERMISSION);
        } else {
          needDialog = true;
        }

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
          ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_PERMISSION);
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

  private void clearDialog() {
    if (mDialog != null && mDialog.isShowing()) {
      mDialog.dismiss();
    }
  }

  private void showOverlayPermissionDialog() {
    clearDialog();
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
      .setTitle(R.string.notification)
      .setMessage(R.string.need_overlay_permission)
      .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          startInstalledAppDetailsActivity(REQ_OVERLAY_PERMISSION);
        }
      }).setNegativeButton(R.string.no_use, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        showSnackbar(R.string.overlay_service_disabled);
        mSettings.setUseOverlay(false);
      }
    });
    mDialog = builder.create();
    mDialog.show();
  }

  private void showLocationPermissionDialog() {
    clearDialog();
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
      .setTitle(R.string.notification)
      .setMessage(R.string.need_location_permission)
      .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          startInstalledAppDetailsActivity(REQ_LOCATION_PERMISSION);
        }
      }).setNegativeButton(R.string.no_use, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        showSnackbar(R.string.location_service_disabled);
        mSettings.setUseLocation(false);
      }
    });
    mDialog = builder.create();
    mDialog.show();
  }

  private void startInstalledAppDetailsActivity(int requestCode) {
    final Intent intent = new Intent();
    intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
    startActivityForResult(intent, requestCode);
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
    Log.i(TAG, "permissions - " + Arrays.toString(permissions));
    Log.i(TAG, "grantResults - " + Arrays.toString(grantResults));
    if (permissions != null) {
      for (int i = 0; i < permissions.length; i++) {
        String permission = permissions[i];
        int grantResult = grantResults[i];
        if (grantResult == PackageManager.PERMISSION_GRANTED
          && (Manifest.permission.ACCESS_FINE_LOCATION.equals(permission) || Manifest.permission.ACCESS_COARSE_LOCATION.equals(permission))) {
          connectGoogleApiClient();

          mSettings.setUseLocation(true);
          showSnackbar(R.string.location_service_enabled);
        } else if (Manifest.permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
          if (grantResult == PackageManager.PERMISSION_DENIED) {
            // 팝업 윈도우 권한 요청 실패시.. shouldShowRequestPermissionRationale가 안 먹히는 케이스가 있음.
            showOverlayPermissionDialog();
          }
        }
      }
    }
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

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  /* Called whenever we call invalidateOptionsMenu() */
  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuItem strides = menu.findItem(R.id.menu_stride);
    MenuItem useOverlay = menu.findItem(R.id.menu_use_overlay);
    MenuItem useLocation = menu.findItem(R.id.menu_use_location);

    strides.setTitle(String.format(getString(R.string.format_menu_strides), mSettings.getStrides()));
    useOverlay.setChecked(mSettings.isUseOverlay());
    useLocation.setChecked(mSettings.isUseLocation());

    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_stride: {
        showStridesInputDialog();
      }
      break;
      case R.id.menu_use_overlay: {
        if (mSettings.isUseOverlay()) {
          mSettings.setUseOverlay(false);
          showSnackbar(R.string.overlay_service_disabled);
        } else {
          if (checkOverlayPermission()) {
            mSettings.setUseOverlay(true);
            showSnackbar(R.string.overlay_service_enabled);
          }
        }
      }
      break;
      case R.id.menu_use_location: {
        if (mSettings.isUseLocation()) {
          mSettings.setUseLocation(false);
          showSnackbar(R.string.location_service_disabled);
        } else {
          if (checkLocationPermission()) {
            mSettings.setUseLocation(true);
            showSnackbar(R.string.location_service_enabled);
          }
        }
      }
      break;
    }
    return super.onOptionsItemSelected(item);
  }

  private void showStridesInputDialog() {
    clearDialog();
    View view = LayoutInflater.from(this).inflate(R.layout.dialog_strides, null);
    final EditText editStrides = (EditText) view.findViewById(R.id.edit_strides);
    if (mSettings.isStridesSetted()) {
      editStrides.setText(String.valueOf(mSettings.getStrides()));
    } else {
      editStrides.setHint(String.valueOf(mSettings.getStrides()));
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.setting)
      .setView(view)
      .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          String input = editStrides.getText().toString();
          try {
            int strides = Integer.parseInt(input);
            mSettings.setStridesSetted(true);
            mSettings.setStrides(strides);

            Intent broadCast = new Intent(Settings.ACTION_STRIDES_CHANGED);
            LocalBroadcastManager.getInstance(mApp).sendBroadcast(broadCast);
          } catch (Exception e) {
            Log.w(TAG, "Integer parse error!");
            showSnackbar(R.string.please_input_number);
          }
        }
      }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        // skip
      }
    });
    mDialog = builder.create();
    mDialog.show();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQ_LOCATION_PERMISSION) {
      if (checkLocationPermission()) {
        mSettings.setUseLocation(true);
        showSnackbar(R.string.location_service_enabled);
      }
    } else if (requestCode == REQ_OVERLAY_PERMISSION) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        boolean permitted = android.provider.Settings.canDrawOverlays(this);
        Log.d(TAG, "onActivityResult - " + permitted);
        if (permitted) {
          mSettings.setUseOverlay(true);
          showSnackbar(R.string.overlay_service_enabled);
        }
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}