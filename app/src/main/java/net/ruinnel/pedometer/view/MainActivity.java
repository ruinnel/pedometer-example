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
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import butterknife.ButterKnife;
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
import net.ruinnel.pedometer.api.NaverMapClient;
import net.ruinnel.pedometer.api.bean.Error;
import net.ruinnel.pedometer.api.bean.ReverseGeocode;
import net.ruinnel.pedometer.util.Log;
import net.ruinnel.pedometer.view.widget.BaseActivity;
import okhttp3.Request;
import okio.Buffer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.inject.Inject;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by ruinnel on 2017. 1. 11..
 */
public class MainActivity extends BaseActivity
  implements NetModule.RequestModifiedListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
  private static final String TAG = MainActivity.class.getSimpleName();

  public static final int INTERVAL = 10 * 60 * 1000;      // 10분
  public static final int DISPLACEMENT = 100;              // 100m
  public static final int USER_PERMISSION_REQUEST = 9876;

  @Inject
  NaverMapClient mAppClient;

  @Inject
  SensorManager mSensorManager;

  @Inject
  Gson mGson;

  private GoogleApiClient mGoogleApiClient;
  private boolean mIsApiConnected;

  private Location mLastLocation;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mApp.component().inject(this);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);

    if (BuildConfig.DEBUG) {
      mApp.setRequestModifiedListener(this);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (checkPlayServices() && checkPermission()) {
      connectGoogleApiClient();
    }

    // TODO test.
    //requestReverseGeocode(37.551633, 127.128662);
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mGoogleApiClient != null) {
      stopLocationUpdates();
      mGoogleApiClient.disconnect();
    }
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

  private void requestReverseGeocode(double lat, double lng) {
    Call<ReverseGeocode> call = mAppClient.reverseGeocode(String.format("%f,%f", lng, lat));
    call.enqueue(new Callback<ReverseGeocode>() {
      @Override
      public void onResponse(Call<ReverseGeocode> call, Response<ReverseGeocode> response) {
        if (response.isSuccessful()) {
          ReverseGeocode.Item item = response.body().result.getFirstItem();
          showSnackbar(item.address);
          Log.i(TAG, "response = " + response.body());
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

    mIsApiConnected = true;
    mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

    if (mLastLocation == null) {
      Log.i(TAG, "Couldn't get the location. Make sure location is enabled on the device.");
    } else {
      Log.i(TAG, "LastLocation " + mLastLocation.getLatitude() + ", " + mLastLocation.getLongitude() + " (" + mLastLocation.getProvider() + ")");
      requestReverseGeocode(mLastLocation.getLatitude(), mLastLocation.getLongitude());
    }

    startLocationUpdates();
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
    requestReverseGeocode(location.getLatitude(), location.getLongitude());
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
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    intent.addCategory(Intent.CATEGORY_DEFAULT);
    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
    this.startActivity(intent);
  }
}