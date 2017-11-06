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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class RecordLocationService extends Service {

  private static final String TAG = "RecordLocationService";

  private FileOutputStream fileOutputStream;
  private PrintWriter printWriter;
  private boolean finishFlowExecuted = false;
  // Binder given to clients
  private final IBinder mBinder = new RecordLocationBinder();
  private ILocationUpdateCallback mLocationUpdateCallback;

  class RecordLocationBinder extends Binder {

    RecordLocationService getService() {
      return RecordLocationService.this;
    }
  }

  private static String gpxHeader =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<gpx version=\"1.0\">\n"
          + "\t<name>Example gpx</name>\n"
          + "\t<trk><name>Track</name><number>1</number><trkseg>";

  private LocationManager locationManager;
  private LocationListener locationListener;

  private void recordLocation(Location location) {
    writeLocationToFile(location);
  }

  public void setLocationUpdateCallback(ILocationUpdateCallback callback) {
    mLocationUpdateCallback = callback;
  }

  private void executeFinishFlow() {
    stopLocationUpdates();
    saveAndCloseFile();
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
    if (printWriter != null) {
      printWriter.println(gpxText);
    }
  }

  private void requestLocationUpdates() {
    // Acquire a reference to the system Location Manager
    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    locationListener =
        new LocationListener() {
          public void onLocationChanged(Location location) {
            recordLocation(location);
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
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    setUpOutputFile();
  }

  public boolean isExternalStorageWritable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
  }

  private void setUpOutputFile() {
    SimpleDateFormat dateTime = new SimpleDateFormat("yyyyMMdd_HHmmss");
    dateTime.setTimeZone(TimeZone.getDefault());
    String currentDateAndTime = dateTime.format(new Date());
    String fileName = currentDateAndTime + ".gpx";
    Log.d(TAG, fileName);

    if (isExternalStorageWritable()) {
      String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/gpx_recorder";
      File storageDir = new File(path);
      storageDir.mkdirs();
      File file = new File(storageDir, fileName);
      try {
        fileOutputStream = new FileOutputStream(file);
        printWriter = new PrintWriter(fileOutputStream);
        Log.d(TAG, "Success opening file.");
        printWriter.println(gpxHeader);
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

  private void stopLocationUpdates() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
      stopSelf();
    }
    // Remove the listener previously added.
    locationManager.removeUpdates(locationListener);
  }

  private void saveAndCloseFile() {
    if (printWriter != null && fileOutputStream != null) {
      printWriter.println("\t</trkseg></trk>\n" + "</gpx>\n");
      printWriter.flush();
      printWriter.close();
      try {
        fileOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      finishFlowExecuted = true;
      Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
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
    if (intent.getAction() == null) {
      Log.e(TAG, "No action provided.");
      stopSelf();
    }
    switch (intent.getAction()) {
      case Constants.Action.ACTION_START:
        Log.i(TAG, "Start is called.");
        showNotification();
        Toast.makeText(this, "Recording location...", Toast.LENGTH_SHORT).show();
        requestLocationUpdates();
        break;
      case Constants.Action.ACTION_STOP:
        Log.i(TAG, "Stop is called.");
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_LONG).show();
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
    if (!finishFlowExecuted) {
      Log.i(TAG, "File not saved! Saving now...");
      executeFinishFlow();
    }
    super.onDestroy();
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
}
