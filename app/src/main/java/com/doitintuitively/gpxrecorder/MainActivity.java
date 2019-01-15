package com.doitintuitively.gpxrecorder;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.DialogFragment;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.doitintuitively.gpxrecorder.BatteryAlertDialogFragment.BatteryAlertDialogListener;
import com.doitintuitively.gpxrecorder.Constants.LocationUpdate;
import com.doitintuitively.gpxrecorder.Constants.Ui;
import com.doitintuitively.gpxrecorder.RecordLocationService.RecordLocationBinder;

/** Main Activity. */
public class MainActivity extends AppCompatActivity implements BatteryAlertDialogListener {

  private static final String TAG = "MainActivity";
  private static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;
  private static final int REQUEST_CODE_ACCESS_FINE_LOCATION = 1;

  private Button mButtonStart;
  private TextView mTextViewLatLong;
  private TextView mTextViewAltitude;
  private TextView mTextViewAccuracy;

  private RecordLocationService mRecordLocationService;
  private SharedPreferences mSharedPreferences;
  private boolean mBound = false;
  private ServiceConnection mConnection =
      new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
          // We've bound to LocalService, cast the IBinder and get LocalService instance
          RecordLocationBinder binder = (RecordLocationBinder) service;
          mRecordLocationService = binder.getService();
          mRecordLocationService.setLocationUpdateCallback(mLocationUpdateCallback);
          mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
          mBound = false;
        }
      };

  private ILocationUpdateCallback mLocationUpdateCallback =
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
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mButtonStart = findViewById(R.id.button_start);
    mButtonStart.setEnabled(false);
    mTextViewLatLong = findViewById(R.id.tv_lat_long);
    mTextViewAltitude = findViewById(R.id.tv_altitude);
    mTextViewAccuracy = findViewById(R.id.tv_accuracy);
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

    checkPermission();
    setUpNotificationChannel();
  }

  private void checkPermission() {
    boolean shouldEnableStart = true;
    if (ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      shouldEnableStart = false;
      requestLocationPermission();
    } else {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
        shouldEnableStart = false;
        requestFilePermission();
      }
    }
    mButtonStart.setEnabled(shouldEnableStart);
  }

  private void setUpUi() {
    if (isServiceRunning(RecordLocationService.class)) {
      setUpRecordingUi();
      // Re-bind to RecordLocationService in case MainActivity was killed and restarted.
      Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
      Log.i(TAG, "re-binding");
      bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    } else {
      mButtonStart.setText(getString(R.string.main_start));
    }
    mButtonStart.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            // If RecordLocationService is running, unbind and stop it.
            if (isServiceRunning(RecordLocationService.class)) {
              String storageDir = mRecordLocationService.getStorageDir();
              String fileName = mRecordLocationService.getFileName();
              Log.i(TAG, "File name retrieved from service: " + storageDir + "/" + fileName);

              Log.i(TAG, "Unbinding service");
              unbindService(mConnection);
              mBound = false;
              Log.i(TAG, "Stopping service");
              Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
              intent.setAction(Constants.Action.ACTION_STOP);
              startService(intent);

              showFileSavedSnackBar(storageDir, fileName);
              tearDownRecordingUi();
            } else {
              checkPermission();
              // Show battery alert before proceeding.
              BatteryAlertDialogFragment dialog = new BatteryAlertDialogFragment();
              if (needToShowBatteryAlert()) {
                dialog.show(getFragmentManager(), "Alert");
              } else {
                setUpRecordingUi();
                startAndBindService();
              }
            }
          }
        });
  }

  private void setUpRecordingUi() {
    mTextViewLatLong.setText(getString(R.string.waiting_for_location));
    mButtonStart.setText(getString(R.string.main_stop));
  }

  private void tearDownRecordingUi() {
    mTextViewLatLong.setText(getString(R.string.recording_stopped));
    mTextViewAltitude.setText("");
    mTextViewAccuracy.setText("");
    mButtonStart.setText(getString(R.string.main_start));
  }

  private void startAndBindService() {
    Log.i(TAG, "Service is not running. Starting service...");

    int locationUpdateMinTime =
        mSharedPreferences.getInt(
            getString(R.string.pref_update_frequency_key), LocationUpdate.MIN_TIME_DEFAULT);
    Log.i(TAG, "locationUpdateMinTime: " + locationUpdateMinTime);

    Intent intent = new Intent(MainActivity.this, RecordLocationService.class);
    intent.setAction(Constants.Action.ACTION_START);
    intent.putExtra(LocationUpdate.MIN_TIME_KEY, locationUpdateMinTime);
    startService(intent);

    // Bind to RecordLocationService.
    bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
  }

  /**
   * Whether we need to show rating agreement.
   *
   * @return Whether we need to show rating agreement.
   */
  private boolean needToShowBatteryAlert() {
    return mSharedPreferences.getBoolean("batteryAlert", true);
  }

  /** Write in SharedPreferences that user don't want to show dialog again. */
  private void doNotShowBatteryAlertAgain() {
    SharedPreferences.Editor editor = mSharedPreferences.edit();
    // Do not show again.
    editor.putBoolean("batteryAlert", false);
    editor.commit();
  }

  /**
   * When user pressed agree.
   *
   * @param dialog BatteryAlertDialogFragment.
   */
  @Override
  public void onDialogPositiveClick(DialogFragment dialog) {
    Log.d(TAG, "User agreed to start.");
    setUpRecordingUi();
    startAndBindService();
  }

  /**
   * When user pressed agree with do not show again.
   *
   * @param dialog BatteryAlertDialogFragment.
   */
  @Override
  public void onDialogPositiveClickWithChecked(DialogFragment dialog) {
    Log.d(TAG, "User agreed to submit and do not show again");
    doNotShowBatteryAlertAgain();
    setUpRecordingUi();
    startAndBindService();
  }

  /**
   * When user pressed cancel.
   *
   * @param dialog BatteryAlertDialogFragment.
   */
  @Override
  public void onDialogNegativeClick(DialogFragment dialog) {
    Log.d(TAG, "Cancel");
    Toast.makeText(getApplicationContext(), "Operation canceled.", Toast.LENGTH_LONG).show();
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
      // Display a SnackBar with an explanation and a button to trigger the request.
      Snackbar.make(
              findViewById(R.id.content_main),
              R.string.main_permission_location_rationale,
              Snackbar.LENGTH_INDEFINITE)
          .setAction(
              R.string.main_ok,
              new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  ActivityCompat.requestPermissions(
                      MainActivity.this,
                      new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                      REQUEST_CODE_ACCESS_FINE_LOCATION);
                }
              })
          .show();
    } else {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
          REQUEST_CODE_ACCESS_FINE_LOCATION);
    }
  }

  private void requestFilePermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
      // Display a SnackBar with an explanation and a button to trigger the request.
      Snackbar.make(
              findViewById(R.id.content_main),
              R.string.main_permission_file_rationale,
              Snackbar.LENGTH_INDEFINITE)
          .setAction(
              R.string.main_ok,
              new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                  ActivityCompat.requestPermissions(
                      MainActivity.this,
                      new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                      REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
                }
              })
          .show();
    } else {
      // No explanation needed, we can request the permission.
      ActivityCompat.requestPermissions(
          this,
          new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
          REQUEST_CODE_WRITE_EXTERNAL_STORAGE);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_CODE_ACCESS_FINE_LOCATION:
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
        checkPermission();
        break;

      case REQUEST_CODE_WRITE_EXTERNAL_STORAGE:
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
          Toast.makeText(this, "Storage permission not granted", Toast.LENGTH_SHORT).show();
        }
        checkPermission();
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
    setUpUi();
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
    mTextViewLatLong.setText(
        String.format(
            getString(R.string.main_lat_long), location.getLatitude(), location.getLongitude()));

    if (location.getProvider().equals("gps")) {
      mTextViewAltitude.setText(
          String.format(getString(R.string.main_altitude), location.getAltitude()));
    }
    mTextViewAccuracy.setText(
        String.format(getString(R.string.main_accuracy), location.getAccuracy()));
  }

  /**
   * Show a SnackBar to let the user know where the file is saved, and share it.
   *
   * @param storageDir Directory containing the saved file.
   * @param fileName Name of the saved file.
   */
  private void showFileSavedSnackBar(String storageDir, String fileName) {
    if (storageDir == null || fileName == null) {
      return;
    }
    final String filePath = storageDir + "/" + fileName;
    String message = getString(R.string.main_file_saved) + " " + filePath + ".";
    Snackbar.make(findViewById(R.id.content_main), message, Ui.FILE_SAVED_SNACK_BAR_DURATION)
        .setAction(
            R.string.main_share,
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(filePath));
                shareIntent.setType("*/*");
                startActivity(
                    Intent.createChooser(shareIntent, getString(R.string.main_share_title)));
              }
            })
        .show();
  }
}
