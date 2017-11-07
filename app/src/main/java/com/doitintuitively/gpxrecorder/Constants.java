package com.doitintuitively.gpxrecorder;

final class Constants {
  static class Notification {
    static final int NOTIFICATION_ID = 1;
    static final String DEFAULT_CHANNEL_ID = "channel 1";
  }

  static class Action {
    static final String ACTION_START = "start";
    static final String ACTION_STOP = "stop";
  }

  static class LocationUpdate {
    static final String MIN_TIME_KEY = "minTime";
    static final int MIN_TIME_DEFAULT = 0;
  }

  static class Gpx {
    static final String GPX_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<gpx version=\"1.0\">\n"
            + "\t<name>Example gpx</name>\n"
            + "\t<trk><name>Track</name><number>1</number><trkseg>";
    static final String GPX_FOOTER = "\t</trkseg></trk>\n" + "</gpx>\n";
  }
}
