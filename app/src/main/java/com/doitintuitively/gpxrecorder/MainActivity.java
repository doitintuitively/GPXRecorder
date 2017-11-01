package com.doitintuitively.gpxrecorder;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

public class MainActivity extends AppCompatActivity {

  private static final String TAG = "MainActivity";
  private static final int MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 2;
  private static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 1;

  private static String gpxHeader =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<gpx version=\"1.0\">\n"
          + "\t<name>Example gpx</name>\n"
          + "\t<trk><name>Track</name><number>1</number><trkseg>";

  private LocationManager locationManager;
  private LocationListener locationListener;
  private Button buttonStart;
  private TextView textViewLatLong;
  private TextView textViewAltitude;
  private TextView textViewAccuracy;

  private boolean started = false;

  private FileOutputStream fileOutputStream;
  private PrintWriter printWriter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      requestLocationPermission();
    }
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      requestFilePermission();
    }

    // Acquire a reference to the system Location Manager
    locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

    // Define a listener that responds to location updates
    locationListener =
        new LocationListener() {
          public void onLocationChanged(Location location) {
            recordLocation(location);
          }

          public void onStatusChanged(String provider, int status, Bundle extras) {}

          public void onProviderEnabled(String provider) {}

          public void onProviderDisabled(String provider) {}
        };

    buttonStart = (Button) findViewById(R.id.button_start);
    buttonStart.setOnClickListener(
        new View.OnClickListener() {
          public void onClick(View v) {
            if (!started) {
              requestLocationUpdates();
            } else {
              stopLocationUpdates();
            }
          }
        });

    textViewLatLong = (TextView) findViewById(R.id.tv_lat_long);
    textViewAltitude = (TextView) findViewById(R.id.tv_altitude);
    textViewAccuracy = (TextView) findViewById(R.id.tv_accuracy);
  }

  private void recordLocation(Location location) {
    writeLocationToFile(location);
    updateLocationText(location);
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

  private void requestLocationUpdates() {
    // Register the listener with the Location Manager to receive location updates
    if (ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      requestLocationPermission();
      Toast.makeText(getApplicationContext(), "permission failed", Toast.LENGTH_SHORT).show();
      return;
    }

    buttonStart.setText(R.string.main_stop);
    started = true;
    //        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10,
    // locationListener);
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

    setUpOutputFile();
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
        printWriter = new PrintWriter(file);
        Log.d(TAG, "Success opening file.");
        printWriter.println(gpxHeader);
      } catch (FileNotFoundException e) {
        Toast.makeText(this, "Unable to create file", Toast.LENGTH_SHORT).show();
        e.printStackTrace();
        finish();
      }
    }
  }

  private void stopLocationUpdates() {
    buttonStart.setText(getString(R.string.main_start));
    started = false;
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
      Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
      finish();
    }
    // Remove the listener you previously added
    locationManager.removeUpdates(locationListener);

    if (printWriter != null && fileOutputStream != null) {
      printWriter.println("\t</trkseg></trk>\n" + "</gpx>\n");
      printWriter.flush();
      printWriter.close();
      try {
        fileOutputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      Toast.makeText(this, "File saved", Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
    switch (requestCode) {
      case MY_PERMISSIONS_ACCESS_FINE_LOCATION:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            finish();
          }
          break;
        }
      case MY_PERMISSIONS_WRITE_EXTERNAL_STORAGE:
        {
          // If request is cancelled, the result arrays are empty.
          if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
            finish();
          }
          break;
        }
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
  }

  @Override
  public void onStop() {
    super.onStop();
  }

  public boolean isExternalStorageWritable() {
    return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
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
