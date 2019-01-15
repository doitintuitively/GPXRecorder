package com.doitintuitively.gpxrecorder;

final class Constants {
  static class File {
    static final String FILE_DIRECTORY = "/gpx_recorder";
  }

  static class Ui {
    static final int FILE_SAVED_SNACK_BAR_DURATION = 5000;
    static final float DEFAULT_ZOOM_LEVEL = 17;
  }

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
            + "<gpx version=\"1.0\" creator=\"GPX Recorder\"\n"
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "xmlns=\"http://www.topografix.com/GPX/1/0\"\n"
            + "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0\n"
            + "http://www.topografix.com/GPX/1/0/gpx.xsd\">\n"
            + "\t<name>GPX</name>\n"
            + "\t<trk><name>Track</name><number>1</number><trkseg>";
    static final String GPX_FOOTER = "\t</trkseg></trk>\n" + "</gpx>\n";
  }
}
