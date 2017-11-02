package com.doitintuitively.gpxrecorder;

import android.location.Location;

public interface ILocationUpdateCallback {
  public void update(Location location);
}
