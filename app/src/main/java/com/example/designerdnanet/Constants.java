package com.example.designerdnanet;

import java.util.UUID;

public class Constants {
    // MAC Address
    public static String MAC_ADDRESS = "B4:52:A9:1B:3B:35";
    public static String MAC_ADDRESS_BLUNO = "50:51:A9:8D:F8:59";
    // Tag name for Log message
    public static String TAG="Central";
    // used to identify adding bluetooth names
    public static int REQUEST_ENABLE_BT = 1;
    // used to request fine location permission
    public static int REQUEST_FINE_LOCATION = 101;
    // used to request COARSE location permission
    public static int REQUEST_COARSE_LOCATION = 101;
    // used to request COARSE location permission
    public static int REQUEST_BLUETOOTH_ADMIN = 101;
    // used to request write external permission
    public static int REQUEST_WRITE_EXTERNAL = 1;
    // used to request read external permission
    public static int REQUEST_READ_EXTERNAL = 2;
    // Stops scanning after 10 seconds.
    public static long SCAN_PERIOD = 5000;

    // service and uuid
    public static String SERVICE_STRING = "0000dfb0-0000-1000-8000-00805f9b34fb";
    public static UUID UUID_TDCS_SERVICE= UUID.fromString(SERVICE_STRING);
    // command uuid
    public static String CHARACTERISTIC_COMMAND_STRING = "0000dfb1-0000-1000-8000-00805f9b34fb";
    public static UUID UUID_CTRL_COMMAND = UUID.fromString( CHARACTERISTIC_COMMAND_STRING );
    // response uuid
    public static String CHARACTERISTIC_RESPONSE_STRING = "0000dfb1-0000-1000-8000-00805f9b34fb";
    public static UUID UUID_CTRL_RESPONSE = UUID.fromString( CHARACTERISTIC_COMMAND_STRING );

    // Beacon
    public static String IBEACON_UID_LAYOUT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    public static String ALL_BEACONS_REGION = "AllBeaconsRegion";
    public static long FOREGROUNDSCANPERIOD = 5500L;

    // Channel
    public static String CHANNEL_ID = "Default Channel";
}
