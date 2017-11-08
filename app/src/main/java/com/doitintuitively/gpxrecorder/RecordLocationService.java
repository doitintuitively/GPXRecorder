package com.doitintuitively.gpxrecorder;

import android.Manifest;
import android.Manifest.permission;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import com.doitintuitively.gpxrecorder.Constants.Gpx;
import com.doitintuitively.gpxrecorder.Constants.LocationUpdate;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Foreground service that records GPS locations to a GPX file. Note that not only does this service
 * execute in the foreground, but also holds the wakelock to prevent the device from sleeping. This
 * impacts battery life, but it's doing what the user has requested, i.e. to record location over
 * time.
 */
public class RecordLocationService extends Service {

  private static final String TAG = "RecordLocationService";

  private FileOutputStream mFileOutputStream;
  private PrintWriter mPrintWriter;
  private boolean mFinishFlowExecuted = false;
  private ILocationUpdateCallback mLocationUpdateCallback;
  private WakeLock mWakeLock;
  private LocationManager mLocationManager;
  private LocationListener mLocationListener;
  private String mFileName;
  private String mStorageDir;
  // Binder given to clients
  private final IBinder mBinder = new RecordLocationBinder();

  class RecordLocationBinder extends Binder {
    RecordLocationService getService() {
      return RecordLocationService.this;
    }
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    Log.i(TAG, "onBind");
    return mBinder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (intent == null || intent.getAction() == null) {
      Log.e(TAG, "No action provided.");
      stopSelf();
    }
    switch (intent.getAction()) {
      case Constants.Action.ACTION_START:
        Log.i(TAG, "Start is called.");
        showNotification();
        setUpWakeLock();
        Toast.makeText(this, "Recording location...", Toast.LENGTH_SHORT).show();
        setUpOutputFile();
        int minTime =
            intent.getIntExtra(LocationUpdate.MIN_TIME_KEY, LocationUpdate.MIN_TIME_DEFAULT);
        requestLocationUpdates(minTime);
        break;
      case Constants.Action.ACTION_STOP:
        Log.i(TAG, "Stop is called.");
        executeFinishFlow();
        stopForeground(true);
        stopSelf();
        break;
      default:
        Log.w(TAG, "Unknown action.");
    }
    return START_STICKY;
  }

  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public void onDestroy() {
    Log.i(TAG, "Service is being destroyed.");
    if (!mFinishFlowExecuted) {
      Log.i(TAG, "File not saved! Saving now...");
      executeFinishFlow();
    }
    super.onDestroy();
  }

  public void setLocationUpdateCallback(ILocationUpdateCallback callback) {
    mLocationUpdateCallback = callback;
  }

  public String getFileName() {
    return mFileName;
  }

  public String getStorageDir() {
    return mStorageDir;
  }

  private void requestLocationUpdates(int minTime) {
    // Acquire a reference to the system Location Manager
    mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    mLocationListener =
        new LocationListener() {
          public void onLocationChanged(Location location) {
            writeLocationToFile(location);
            if (mLocationUpdateCallback != null) {
              mLocationUpdateCallback.update(location);
            }
          }

          public void onStatusChanged(String provider, int status, Bundle extras) {}

          public void onProviderEnabled(String provider) {}

          public void onProviderDisabled(String provider) {}
        };

    if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(getApplicationContext(), "Permission not granted.", Toast.LENGTH_SHORT).show();
      stopSelf();
    }
    Log.i(TAG, "Requesting location updates with minTime = " + minTime + " sec.");
    mLocationManager.requestLocationUpdates(
        LocationManager.GPS_PROVIDER, minTime * 1000, 0, mLocationListener);
  }

  public boolean isExternalStorageWritable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

  private void setUpOutputFile() {
    SimpleDateFormat dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss");
    dateTime.setTimeZone(TimeZone.getDefault());
    String currentDateAndTime = dateTime.format(new Date());
    mFileName = currentDateAndTime + ".gpx";
    Log.d(TAG, "File name: " + mFileName);

    if (isExternalStorageWritable()) {
      mStorageDir =
          Environment.getExternalStorageDirectory().getAbsolutePath()
              + Constants.File.FILE_DIRECTORY;
      File storageDir = new File(mStorageDir);
      storageDir.mkdirs();
      File file = new File(storageDir, mFileName);
      try {
        mFileOutputStream = new FileOutputStream(file);
        mPrintWriter = new PrintWriter(mFileOutputStream);
        Log.d(TAG, "Success opening file.");
        mPrintWriter.println(Gpx.GPX_HEADER);
      } catch (FileNotFoundException e) {
        Toast.makeText(this, "Unable to create file", Toast.LENGTH_SHORT).show();
        Log.e(TAG, e.toString());
        stopSelf();
      }
    } else {
      Toast.makeText(getApplicationContext(), "Cannot write to storage.", Toast.LENGTH_SHORT)
          .show();
      stopSelf();
    }
  }

  private void showNotification() {
    Intent notificationIntent = new Intent(this, MainActivity.class);
    PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

    Notification notification =
        new NotificationCompat.Builder(this, Constants.Notification.DEFAULT_CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();

    startForeground(Constants.Notification.NOTIFICATION_ID, notification);
  }

  private void setUpWakeLock() {
    PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
    mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
    mWakeLock.acquire();
  }

  private void writeLocationToFile(Location location) {
    String gpxText =
        "\t\t<trkpt lat=\""
            + location.getLatitude()
            + "\" lon=\""
            + location.getLongitude()
            + "\">";
    if (location.getProvider().equals("gps")) {
      gpxText += "<ele>" + location.getAltitude() + "</ele>";
    }
    DateFormat dateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    String time = dateTime.format(new Date(location.getTime()));
    gpxText += "<time>" + time + "</time></trkpt>";
    if (mPrintWriter != null) {
      mPrintWriter.println(gpxText);
    }
  }

  private void executeFinishFlow() {
    stopLocationUpdates();
    saveAndCloseFile();
    mWakeLock.release();
  }

  private void stopLocationUpdates() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
      stopSelf();
    }
    // Remove the listener previously added.
    mLocationManager.removeUpdates(mLocationListener);
  }

  private void saveAndCloseFile() {
    if (mPrintWriter != null && mFileOutputStream != null) {
      mPrintWriter.println(Gpx.GPX_FOOTER);
      mPrintWriter.flush();
      mPrintWriter.close();
      try {
        mFileOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      mFinishFlowExecuted = true;
    }
  }
}
