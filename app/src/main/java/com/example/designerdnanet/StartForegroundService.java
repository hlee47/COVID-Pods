package com.example.designerdnanet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static com.example.designerdnanet.Constants.ALL_BEACONS_REGION;
import static com.example.designerdnanet.Constants.CHANNEL_ID;
import static com.example.designerdnanet.Constants.FOREGROUNDSCANPERIOD;
import static com.example.designerdnanet.Constants.IBEACON_UID_LAYOUT;
import static com.example.designerdnanet.Constants.REQUEST_ENABLE_BT;

public class StartForegroundService extends Service implements BeaconConsumer {
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private BeaconManager beaconManager;
    private BeaconTransmitter beaconTransmitter;
    private BeaconParser beaconParser;
    private Beacon beacon;
    private List<String> beaconsInRegionArrayList = new ArrayList<>();
    private Region region;
    private final static String uniqueId = UUID.randomUUID().toString();
    // Notification
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private static Notification notification;
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("***** foreground service started *****");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //여기 시작 String input = intent.getStringExtra("inputExtra"); //인텐트 값 //안드로이드 O버전 이상에서는 알림창을 띄워야 포그라운드 사용 가능

            System.out.println("***** foreground service started *****");
//            Intent notificationIntent = new Intent(this, MainActivity.class);
////            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
////            Notification notification = new NotificationCompat.Builder(this, "default")
////                    .setContentTitle("Foreground Service")
////                    .setContentText("Test Foreground")
////                    .setSmallIcon(R.mipmap.ic_launcher)
////                    .setContentIntent(pendingIntent)
////                    .build();
////
////            createNotificationChannel();
////            startForeground(1, notification);
////            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
////            manager.notify(1,notification);
////
//            prepareBeacon();
//            prepareBeaconDetection();
//            beaconTransmitter("3");
            //do heavy work on a background thread

            // stopSelf();
            // /* START_STICKY : Service가 강제 종료되었을 경우 시스템이 다시 Service를 재시작 시켜 주지만 intent 값을 null로 초기화 시켜서 재시작 합니다.
            // Service 실행시 startService(Intent service) 메서드를 호출 하는데 onStartCommand(Intent intent, int flags, int startId) 메서드에 intent로 value를 넘겨 줄 수 있습니다.
            // 기존에 intent에 value값이 설정이 되있다고 하더라도 Service 재시작시 intent 값이 null로 초기화 되서 재시작 됩니다.
            // START_NOT_STICKY : 이 Flag를 리턴해 주시면, 강제로 종료 된 Service가 재시작 하지 않습니다.
            // 시스템에 의해 강제 종료되어도 괸찮은 작업을 진행 할 때 사용해 주시면 됩니다.
            // 출처: https://hashcode.co.kr/questions/1082/%EC%84%9C%EB%B9%84%EC%8A%A4%EC%97%90%EC%84%9C-start_sticky%EC%99%80-start_not_sticky%EC%9D%98-%EC%B0%A8%EC%9D%B4%EB%8A%94-%EB%AD%94%EA%B0%80%EC%9A%94 */

        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    @Nullable @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel( new NotificationChannel( "default", "Foreground Service Channel", NotificationManager.IMPORTANCE_HIGH ));
    }

    private void prepareBeacon() {
        //// Beacon
        beaconManager = BeaconManager.getInstanceForApplication(this);
        BeaconParser iBeacon = new BeaconParser();
        iBeacon.setBeaconLayout(IBEACON_UID_LAYOUT);
        BeaconParser altBeacon = new BeaconParser();
        altBeacon.setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);
        BeaconParser eddyStoneBeacon = new BeaconParser();
        eddyStoneBeacon.setBeaconLayout(BeaconParser.ALTBEACON_LAYOUT);
        beaconManager.getBeaconParsers().add(iBeacon);
        beaconManager.getBeaconParsers().add(altBeacon);
        beaconManager.getBeaconParsers().add(eddyStoneBeacon);
        ArrayList<Identifier> identifiers = new ArrayList<>();
        region = new Region(ALL_BEACONS_REGION, identifiers);
        beaconManager.setForegroundScanPeriod(FOREGROUNDSCANPERIOD);
        beaconManager.setForegroundBetweenScanPeriod(BeaconManager.DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD);
        beaconManager.bind((BeaconConsumer) this);
    }

    private Beacon beaconBuilder(Beacon beacon, String majorValue) {
        beacon = new Beacon.Builder()
                .setId1(uniqueId)
                .setId2(majorValue)
                .setId3("1")
                .setManufacturer(0x0001) // Choose a number of 0x00ff or less as some devices cannot detect beacons with a manufacturer code > 0x00ff
                .setTxPower(-59)
                .setDataFields(Arrays.asList(new Long[] {2222L}))
                .build();
        return beacon;
    }

    private void beaconTransmitter(String majorValue) {
        // Sets up to transmit as an AltBeacon-style beacon.  If you wish to transmit as a different
        // type of beacon, simply provide a different parser expression.  To find other parser expressions,
        // for other beacon types, do a Google search for "setBeaconLayout" including the quotes
        beaconTransmitter = new BeaconTransmitter(this, new BeaconParser().setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));

        // Transmit a beacon with Identifiers 2F234454-CF6D-4A0F-ADF2-F4911BA9FFA6 1 2
        beacon = beaconBuilder(beacon, majorValue);

        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                super.onStartSuccess(settingsInEffect);
                System.out.println("***** Beacon Advertise Succeed: " + beacon.getDataFields().toString() + " *****");

            }

            @Override
            public void onStartFailure(int errorCode) {
                super.onStartFailure(errorCode);
                System.out.println("***** Beacon Advertise Failed *****");
            }
        });
    }

    private void prepareBeaconDetection() {

        startBeaconMonitoring();

    }

    private void startBeaconMonitoring() {
        System.out.println("***** Start beacon monitoring *****");
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            System.out.println("***** beacon monitoring exception: " + e + " *****");
        }
    }

    @Override
    public void onBeaconServiceConnect() {
        beaconManager.removeAllRangeNotifiers();
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> collection, Region region) {
                if (collection.size() > 0) {
                    List<Beacon> detectedBeacons = new ArrayList<>();
                    detectedBeacons.addAll(collection);
                    for (Beacon beacon : detectedBeacons) {
                        Identifier beaconMajorIdentifier = beacon.getId2();
                        if (beaconMajorIdentifier.toInt() == 3) {
                            notification = createRandomNotification();
                            showNotification(notification);
                        }
                    }

                } else {
                    System.out.println("***** Beacon(s) in region: 0 *****");
                }
            }

        });
    }

    private void showNotification(Notification notification) {
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, notification);
    }

    private Notification createRandomNotification() {
        Intent intent = new Intent(this, StartForegroundService.class);
        intent.setAction("startForeground");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        System.out.println(" ***** create notification channel");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification =  builder.setSmallIcon(R.mipmap.ic_main)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main))
                .setContentTitle("test")
                .setContentText(UUID.randomUUID().toString())
                .setColor(getResources().getColor(R.color.colorOrangeF79256))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true).build();
        // 알림 표시
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "기본 채널", NotificationManager.IMPORTANCE_HIGH));
        notificationManager.notify(1, builder.build());
        return notification;
    }
}