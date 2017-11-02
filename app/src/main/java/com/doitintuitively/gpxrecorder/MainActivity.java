package com.doitintuitively.gpxrecorder;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.doitintuitively.gpxrecorder.RecordLocationService.RecordLocationBinder;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";
  private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 2;
  private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

  private Button buttonStart;
  private TextView textViewLatLong;
  private TextView textViewAltitude;
  private TextView textViewAccuracy;

  private RecordLocationService mRecordLocationService;
  private boolean mBound = false;
  private ServiceConnection mConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
          // We've bound to LocalService, cast the IBinder and get LocalService instance
          RecordLocationBinder binder = (RecordLocationBinder) service;
          mRecordLocationService = binder.getService();
          mRecordLocationService.setLocationUpdateCallback(locationUpdateCallback);
          mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
          mBound = false;
        }
      };

  private ILocationUpdateCallback locationUpdateCallback =
      new ILocationUpdateCallback() {
        @Override
        public void update(Location location) {
          updateLocationText(location);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    checkPermission();

    buttonStart = (Button) findViewById(R.id.button_start);
    textViewLatLong = (TextView) findViewById(R.id.tv_lat_long);
    textViewAltitude = (TextView) findViewById(R.id.tv_altitude);
    textViewAccuracy = (TextView) findViewById(R.id.tv_accuracy);

    setUpNotificationChannel();
  }

  private void checkPermission() {
    if (ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(getApplicationContext(), "Please grant permission.", Toast.LENGTH_SHORT)
          .show();
      requestLocationPermission();
    }
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(getApplicationContext(), "Please grant permission.", Toast.LENGTH_SHORT)
          .show();
      requestFilePermission();
    }
  }

  private void setUpStartButton() {
    if (isServiceRunning(RecordLocationService.class)) {
      buttonStart.setText(getString(R.string.main_stop));
      // Re-bind to RecordLocationService in case MainActivity was killed and restarted.
      Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
      Log.i(TAG, "re-binding");
      bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    } else {
      buttonStart.setText(getString(R.string.main_start));
    }
    buttonStart.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            // If RecordLocationService is running, unbind and stop it.
            if (isServiceRunning(RecordLocationService.class)) {
              Log.i(TAG, "Unbinding service");
              unbindService(mConnection);
              mBound = false;

              Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
              intent.setAction(Constants.Action.ACTION_STOP);
              startService(intent);

              buttonStart.setText(getString(R.string.main_start));
            } else {
              checkPermission();
              Log.i(TAG, "Service is not running. Starting service...");

              Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
              intent.setAction(Constants.Action.ACTION_START);
              startService(intent);

              // Bind to RecordLocationService.
              bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

              buttonStart.setText(getString(R.string.main_stop));
            }
          }
        });
  }

  private void setUpNotificationChannel() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      NotificationManager mNotificationManager =
          (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
      NotificationChannel mChannel;
      mChannel =
          new NotificationChannel(
              Constants.Notification.DEFAULT_CHANNEL_ID,
              getString(R.string.default_notification_channel_name),
              NotificationManager.IMPORTANCE_LOW);
      // Configure the notification channel.
      mChannel.setDescription(getString(R.string.default_notification_channel_description));
      mChannel.enableLights(false);
      mChannel.enableVibration(false);
      mNotificationManager.createNotificationChannel(mChannel);
    }
  }

  private boolean isServiceRunning(Class<?> serviceClass) {
    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }

  private void requestLocationPermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.ACCESS_FINE_LOCATION)) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          MY_PERMISSIONS_ACCESS_FINE_LOCATION);
    } else {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          MY_PERMISSIONS_ACCESS_FINE_LOCATION);
    }
  }

  private void requestFilePermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
          MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
    } else {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
          MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSIONS_ACCESS_FINE_LOCATION:
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
          finish();
        }
        break;

      case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE:
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
          finish();
        }
        break;
      default:
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
      startActivity(startSettingsActivity);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onStart() {
    super.onStart();
    setUpStartButton();
  }

  @Override
  public void onStop() {
    super.onStop();
    // Unbind service if activity is being stopped.
    if (mBound) {
      Log.i(TAG, "Unbinding service");
      unbindService(mConnection);
      mBound = false;
    }
  }

  private void updateLocationText(Location location) {
    textViewLatLong.setText(
        String.format(
            getString(R.string.main_lat_long), location.getLatitude(), location.getLongitude()));

    if (location.getProvider().equals("gps")) {
      textViewAltitude.setText(
          String.format(getString(R.string.main_altitude), location.getAltitude()));
    }
    textViewAccuracy.setText(
        String.format(getString(R.string.main_accuracy), location.getAccuracy()));
  }
}
