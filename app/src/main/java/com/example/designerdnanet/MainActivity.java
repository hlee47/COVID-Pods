package com.example.designerdnanet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

// bluetooth
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

// chart
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

// firebase
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
import com.google.common.util.concurrent.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

// Beacon
import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

// other
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.example.designerdnanet.Constants.ALL_BEACONS_REGION;
import static com.example.designerdnanet.Constants.CHANNEL_ID;
import static com.example.designerdnanet.Constants.FOREGROUNDSCANPERIOD;
import static com.example.designerdnanet.Constants.IBEACON_UID_LAYOUT;
import static com.example.designerdnanet.Constants.REQUEST_ENABLE_BT;
import static com.example.designerdnanet.Constants.UUID_TDCS_SERVICE;
import static com.example.designerdnanet.Constants.UUID_CTRL_COMMAND;

public class MainActivity extends AppCompatActivity implements
        OnChartValueSelectedListener, IAxisValueFormatter, BeaconConsumer {

    // FireBase
    private FirebaseFirestore db;
    private CollectionReference documentReferenceCOVIDPatient;
    private static DocumentReference documentReferenceUsers;
    private FirebaseStorage firebaseStorage;
    private FirebaseAuth firebaseAuth;
//    private DatabaseReference rootReference;
//    private StorageReference storageReferenceTxt;
//    private StorageReference storageReferenceChart;
//    private DatabaseReference conditionReference;
//    private static String positivePatientUUID;
//    private String positivePatientTestTime;
    private String beaconTimeStampData;
    private static String userId;
    private static boolean isTestedPositive;
    private static String testResult = "0";
    // Chart
    private LineChart lineChart;
    private int lineChartCounter = 0;
    private String receivedData;
    private List<Entry> sampleDataList;
    private StringWriter stringWriter;
    // Bluetooth
    private BluetoothDevice bluetoothDevice;
    private BluetoothGatt bluetoothGatt;
    private String deviceAddress;
    private BluetoothManager bluetoothManager;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    // BluetoothLe
    private BluetoothLeScanner bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    private static boolean beaconScanningEnabled;
    private Handler bleHandler = new Handler();
    private static final long SCAN_PERIOD = 50000;
//    private LeDeviceListAdapter leDeviceListAdapter;
    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
//                    leDeviceListAdapter.addDevice(result.getDevice());
//                    leDeviceListAdapter.notifyDataSetChanged();

                }
            };
    // flag for connection
    private boolean connected = false;
    private boolean initialized = false;
    private String deviceName = null;
    private BluetoothGattCharacteristic characteristic;
    public static Handler handler;
    public static BluetoothSocket mmSocket;
    public static ConnectedThread connectedThread;
    private final static int CONNECTING_STATUS = 1; // used in bluetooth handler to identify message status
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    // altbeacon
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
    // Other
    private ProgressBar progressBar;
    final protected static char[] decimalArray = "0123456789".toCharArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase
        if (userId == null) {
            String userIdfromActivity = getIntent().getStringExtra("UID");
            userId = userIdfromActivity;
        }
        db = FirebaseFirestore.getInstance();
        System.out.println("***** mainactivity oncreate userid: "+userId);
        documentReferenceUsers = db.collection("users").document(userId);
        createUserOnFirestore(db, userId);
        createPatientOnFirestore(db, "0");
        documentReferenceCOVIDPatient = db.collection("COVID19PositivePatient");
//        rootReference = FirebaseDatabase.getInstance().getReference();
//        conditionReference = rootReference.child("condition");
        addFirestoreSnapshot();


        // UI Initialization
        final Button buttonScan = findViewById(R.id.buttonScan);
        final Button buttonStopTest = findViewById(R.id.buttonStopTest);
        final Button buttonSave = findViewById(R.id.buttonSave);
        final Button buttonLeScan = findViewById(R.id.buttonLeScan);
        final Button buttonCOVID19Positive = findViewById(R.id.buttonCOVIDPositive);
        final Button buttonCOVID19Negative = findViewById(R.id.buttonCOVIDNegative);
        final Button buttonLaser = findViewById(R.id.buttonLaser);
        final Button buttonStartTest = findViewById(R.id.buttonStartTest);
        final Button buttonLogout = findViewById(R.id.buttonLogout);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final TextView textViewInfo = findViewById(R.id.textViewInfo);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);


        // Beacon
//        prepareBeacon();
//        beaconTransmitter("1");
//        notification = createRandomNotification();

        // Chart
        drawChart();
        stringWriter = new StringWriter();

        // If a bluetooth device has been selected from SelectDeviceActivity
        deviceName = getIntent().getStringExtra("deviceName");
        if (deviceName != null){
            bluetoothDevice = getIntent().getExtras().getParcelable("bluetoothDevice");
            // Get the device address to make BT Connection
            deviceAddress = getIntent().getStringExtra("deviceAddress");
            // Show progress and connection status
            toolbar.setSubtitle("Connecting to " + deviceName + "...");
            progressBar.setVisibility(View.VISIBLE);
            /*
            This is the most important piece of code. When "deviceName" is found
            the code will call a new thread to create a bluetooth connection to the
            selected device (see the thread code below)
             */
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            // createConnectThread = new CreateConnectThread(bluetoothAdapter,deviceAddress);
            // createConnectThread.start();
            connectDevice(bluetoothDevice);
        }

        /*
        Second most important piece of Code. GUI Handler
         */
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg){
                switch (msg.what){
                    case CONNECTING_STATUS:
                        switch(msg.arg1){
                            case 1:
                                toolbar.setSubtitle("Connected to " + deviceName);
                                progressBar.setVisibility(View.GONE);
                                buttonStopTest.setEnabled(true);
                                break;
                            case -1:
                                toolbar.setSubtitle("Device fails to connect");
                                progressBar.setVisibility(View.GONE);
                                break;
                        }

                        if (beaconScanningEnabled) {
                            System.out.println("***** connected and back to scanning *****");
                            prepareBeacon();
                            prepareBeaconDetection();
                            if (testResult.equals("3")) {
                                System.out.println("***** istestedpositive true *****");
                                beaconTransmitter("3");
                            }
                        } else {
                            System.out.println("***** connected but not back to scanning *****");
                        }

                        break;

                    case MESSAGE_READ:
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        switch (arduinoMsg.toLowerCase()){
                            case "led is turned on":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                            case "led is turned off":
                                textViewInfo.setText("Arduino Message : " + arduinoMsg);
                                break;
                        }
                        break;
                }
            }
        };

        // Firebase messaging service
        // https://blog.naver.com/ndb796/221553341369
        FirebaseInstallations.getInstance().getId().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    System.out.println("***** Get Instance ID from firebase failed *****");
                    return;
                }
                String token = task.getResult();
                System.out.println("***** FCM Token: " + token + " *****");
                showToastMessage(token);
            }
        });

        buttonLeScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(beacon.getId2().toString().equals("0")) {
                    showToastMessage("Please take a test first.");
                } else {
                    prepareBeaconDetection();
                    showToastMessage("Beacon detection started");
//                    Intent intent = new Intent(MainActivity.this, StartForegroundService.class);
//                    intent.setAction("startForeground");
//                    startForegroundService(intent);
                }
            }
        });

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(beaconTransmitter.isStarted()) {
                    beaconTransmitter.stopAdvertising();
                    stopBeaconMonitoring();
                }
                Intent intent = new Intent(MainActivity.this, ScanDeviceActivity.class);
                if(bluetoothDevice!=null) {
                    intent.putExtra("connectedDevice", bluetoothDevice);
                }
                startActivity(intent);
            }
        });

        buttonLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signOut();
                Intent intent = new Intent(MainActivity.this, PhoneAuthenticationActivity.class);
                intent.putExtra("isLoggedIn", true);
                startActivity(intent);
            }
        });

        buttonStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBluetoothData("1");
            }
        });

        buttonStopTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBluetoothData("0");
            }
        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sampleDataList != null) {
                    uploadChartImage(createCurrentTimeName(".jpeg"));
                    createTextFile(createCurrentTimeName(userId+"ChartData.txt"), sampleDataList);
                } else {
                    showToastMessage("Please perform a test");
                }
            }
        });

        buttonCOVID19Positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (beaconManager != null) {
                    if (beaconTransmitter.isStarted()) {
                        beaconTransmitter.stopAdvertising();
                        stopBeaconMonitoring();
                    }
                }
                prepareBeacon();
                prepareBeaconDetection();
                beaconTransmitter("3");
                createPatientOnFirestore(db, "3");
                testResult = "3";
            }
        });

        buttonCOVID19Negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (beaconManager != null) {
                    if (beaconTransmitter.isStarted()) {
                        beaconTransmitter.stopAdvertising();
                        stopBeaconMonitoring();
                    }
                }
                prepareBeacon();
                prepareBeaconDetection();
                beaconTransmitter("1");
                createPatientOnFirestore(db,"1");
                testResult = "1";
            }
        });

        buttonLaser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendBluetoothData("3");
            }
        });

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
//        beaconManager.enableForegroundServiceScanning(createRandomNotification(), 1);
        beaconManager.bind(this);
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

    private void scanBeaconTimeStamp(String positivePatientUUID) {
        System.out.println("***** before timestamp userid: " + userId);
        System.out.println("***** before timestamp uuid: " + positivePatientUUID);
//        System.out.println("***** Timestamp Data: " + beaconTimeStampData);
        if (!beaconTimeStampData.isEmpty()) {
            List<String> beaconTimeStampDataList = new ArrayList<>(Arrays.asList(beaconTimeStampData.split(",")));
            List<String> contactTimeList = new ArrayList<>();
            for (String beacon : beaconTimeStampDataList) {
                if (beacon.matches("(.*)"+positivePatientUUID+"(.*)")) {

                    String beaconTimeStamp = beacon.substring(beacon.lastIndexOf("time: ")+6);
                    contactTimeList.add(beaconTimeStamp);
                }
            }
            System.out.println("*****  contact time list: " + contactTimeList);
            if (!contactTimeList.isEmpty()) {
                createNotificationRecentPositive();
            }
        }
    }

    private void prepareBeaconDetection() {
        if (!isLocationEnabled()) {
            askToTurnOnLocation();
        } else {
            // Localization activity
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                System.out.println("***** Bluetooth Adapter is null *****");
            } else if (bluetoothAdapter.isEnabled()) {
                startBeaconMonitoring();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    private void startBeaconMonitoring() {
        beaconScanningEnabled = true;
        System.out.println("***** Start beacon monitoring *****");
        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
            beaconManager.startRangingBeaconsInRegion(region);
        } catch (RemoteException e) {
            System.out.println("***** beacon monitoring exception: " + e + " *****");
        }
    }

    private void stopBeaconMonitoring() {
        System.out.println("***** Stop beacon monitoring *****");
        beaconManager.removeAllRangeNotifiers();
        beaconManager.removeAllMonitorNotifiers();
        beaconManager.unbind(MainActivity.this);
        beaconManager.disableForegroundServiceScanning();
        beaconManager = null;
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
//                            createNotificationWithinRange();
                            showToastMessage("COVID19 Positive patient is within  your range");
                        }
                    }
                    String detectedBeaconsString = detectedBeacons.toString();
                    List<String> beaconsInRegionArrayListWithTime = new ArrayList<>();
                    detectedBeaconsString = detectedBeaconsString.replace("]","").replace("[","");
                    List<String> beaconsInRegionArrayListWithoutTime = Arrays.asList(detectedBeaconsString.split(","));
                    for (String beaconInRegion : beaconsInRegionArrayListWithoutTime) {
                        String beaconInRegionTime = createCurrentTimeName("");
                        String beaconInRegionWithTime =  beaconInRegion + " time: " + beaconInRegionTime;
                        beaconsInRegionArrayListWithTime.add(beaconInRegionWithTime);
                    }
                    beaconsInRegionArrayList.add(beaconsInRegionArrayListWithTime.toString());
                    System.out.println("***** Beacon(s) in region: " + beaconsInRegionArrayList + " *****");
                    createTextFile( userId + "BeaconInRegionData.txt", beaconsInRegionArrayList);
                } else {
                    System.out.println("***** Beacon(s) in region: 0 *****");
                }
            }

        });
    }

    private void createUserOnFirestore(FirebaseFirestore db, String UID) {
        documentReferenceUsers = db.collection("users").document(userId);
        Map<String, Object> user = new HashMap<>();
        user.put("UID", userId);
        user.put("UUID", uniqueId);
        documentReferenceUsers.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                System.out.println("***** user profile is created *****");
            }
        });
    }

    private void createPatientOnFirestore(FirebaseFirestore db, String Major) {
        Map<String, Object> user = new HashMap<>();
        user.put("UUID", uniqueId);
        user.put("MAJOR", Major);
        user.put("TIME", createCurrentTimeName(""));
        if(beaconManager==null) {
            prepareBeacon();
            beaconTransmitter("0");
        }

        db.collection("COVID19PositivePatient")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        System.out.println("***** DocumentSnapshot added with ID: " + documentReference.getId() + " *****");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("***** Error adding document: " + e.toString() + " *****");
                    }
                });
    }

    private void addFirestoreSnapshot() {
        documentReferenceCOVIDPatient.whereEqualTo("MAJOR","3").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    System.out.println("**** Firestore Listen Failed *****");
                    return;
                }
                if (value != null) {
                    System.out.println("***** someone gets tested positive *****");
                    List<DocumentSnapshot> snapshotList = value.getDocuments();
                    for (DocumentSnapshot snapshot : snapshotList) {
                        System.out.println("***** onEvent: " + snapshot.getData() + " *****");
                        try {
                            String positivePatientUUID = snapshot.getData().get("UUID").toString();
                            String positivePatientTestTime = snapshot.getData().get("TIME").toString();
                            System.out.println("***** Positive Patient. Scanning beacon timestamp with usr UUID & Time: " + positivePatientUUID + " / " + positivePatientTestTime + " ******");
                            downloadFileForScanBeaconTimeStamp(userId+"BeaconInRegionData.txt", positivePatientUUID);
                        } catch (Exception e) {
                            System.out.println("***** Error retrieving UUID and time data from firestore *****");
                        }
                    }
                }
            }
        });

    }



    @Override
    public void onBackPressed() {
        // Terminate Bluetooth Connection and close app
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        beaconManager = BeaconManager.getInstanceForApplication(this);
//        beaconManager.unbind(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (beaconScanningEnabled) {
            prepareBeacon();
            prepareBeaconDetection();
            switch (testResult) {
                case "3" :
                    beaconTransmitter("3");
                    return;
                case "2" :

                case "1" :
                    beaconTransmitter("1");
                    return;
            }
            System.out.println("***** on start : connected and back to scanning *****");
        }
//        addFirestoreSnapshot();

//        conditionReference.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                String text = snapshot.getValue(String.class);
//                System.out.println("***** Data Changed *****");
//                showToastMessage(" Data Changed: " + snapshot.getValue());
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }



    private void askToTurnOnLocation() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setMessage("Location permission is disabled");
        dialog.setPositiveButton("Enable Location Setting", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                // TODO Auto-generated method stub
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean networkLocationEnabled = false;
        boolean gpsLocationEnabled = false;
        try {
            networkLocationEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            gpsLocationEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            System.out.println("***** Error: " + e + " *****");
        }
        return networkLocationEnabled || gpsLocationEnabled;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }



    @Override
    public void onValueSelected(Entry e, Highlight h) {
        System.out.println("***** Entry selected : "+ e.toString() + " *****");
    }

    @Override
    public void onNothingSelected() {
        System.out.println("***** Nothing selected *****");
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return null;
    }



    /* =============================== Thread for Data Transfer =========================================== */
    public static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n'){
                        readMessage = new String(buffer,0,bytes);
                        Log.e("Arduino Message",readMessage);
                        handler.obtainMessage(MESSAGE_READ,readMessage).sendToTarget();
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error","Unable to send message",e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }


    /*======================================================================================*/
    /*=============== https://steemit.com/kr/@etainclub/android-app-ble-6-ble ==============*/

    // Connect to the BLE device
    private void connectDevice(BluetoothDevice bluetoothDevice) {
        // update the status
        GattClientCallback gattClientCallback = new GattClientCallback();
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattClientCallback);
    }

    // Gatt Client Callback Class
    class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if(status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer();
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnectGattServer();
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // update the connection status message
                // set the connection flag
                connected = true;
                System.out.println("***** Connected to the GATT server *****");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            // Check if the discovery failed
            if(status != BluetoothGatt.GATT_SUCCESS) {
                System.out.println("***** Device service discovery failed, status: "+ status +" *****");
                return;
            }
            // find discovered characteristics
            List<BluetoothGattCharacteristic> matchingCharacteristics = BluetoothUtils.findBLECharacteristics(bluetoothGatt);
            BluetoothGattService service = bluetoothGatt.getService(UUID_TDCS_SERVICE);
            characteristic = service.getCharacteristic(UUID_CTRL_COMMAND);
            initialized = bluetoothGatt.setCharacteristicNotification(characteristic, true);
            if(characteristic == null) {
                System.out.println("***** Unable to find characteristics *****");
            } else {
                System.out.println("***** Characteristics: "+ characteristic.toString() +" *****");
            }
            if (matchingCharacteristics.isEmpty()) {
                System.out.println("***** Unable to find characteristics *****");
                return;
            }
            // log for successful discovery
            System.out.println("***** Services discover is successful ******");
            handler.obtainMessage(CONNECTING_STATUS, 1, -1).sendToTarget();
            sampleDataList = new ArrayList<>();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            System.out.println("***** Characteristic changed: "+ characteristic.toString() +" *****");
            System.out.println("***** value is: "+ Arrays.toString(characteristic.getValue()) +" *****");

            readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                byte[] cmdBytes = new byte[];
//                cmdBytes[0] = 1;
//                cmdBytes[1] = 2;
//                characteristic.setValue(cmdBytes);
//                Boolean writeStatus = bluetoothGatt.writeCharacteristic(characteristic);
                System.out.println("***** Characteristic written successfully *****");
            } else {
                System.out.println("***** Characteristic write failed, status: "+ status +" *****");
                disconnectGattServer();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                System.out.println("***** Characteristic read successfully *****");
                readCharacteristic(characteristic);
            } else {
                System.out.println("***** Characteristic read failed, status: "+ status +" *****");
                // Trying to read from the Time Characteristics? It doesn't have the property or permissions
                // set to allow this. Normally this would be an error and you would want to:
                // disconnectGattServer();
            }
        }

        // log the value of the characteristic @param characteristic
        private void readCharacteristic(BluetoothGattCharacteristic bluetoothGattCharacteristic) {
            byte[] msgBytes = bluetoothGattCharacteristic.getValue();
            String msgString = null;
            String msgString2 = null;
            int msgInt;
            try {
                System.out.println("***** Read1: "+ msgBytes +" ******");
                msgString = new String(msgBytes, "UTF-8");
                System.out.println("***** Read2: "+ msgString +" ******");
//                msgString2 = bytesToDecimal(msgBytes);
//                byte msb = (msgBytes[0]);
//                byte lsb = (msgBytes[1]);
                // int val = ~(((msb << 8) | lsb) - 1);
                // System.out.println("***** Read0: "+ ~(((msb<<8) | lsb) -1 ) +" ******");
//                int val = ((msb << 8) | (lsb & 0xFF)) & 0xFFFF;
                System.out.println("***** Read0: "+ msgString +" ******");
//                System.out.println("***** Read0: "+ val +" ******");
                stringWriter.append(String.valueOf(msgString.trim()));
                System.out.println("***** Read2 : "+ stringWriter.toString() +" ******");
                try {
                    msgInt = Integer.parseInt(msgString.trim());
                    sampleDataList.add(new Entry(msgInt, ++lineChartCounter));
                    System.out.println("***** DataList: " + sampleDataList + " *****");
                    addEntry(msgInt);

//                    msgString2 = msgString2.replace(" ", "");
//                    msgInt = Integer.parseInt(msgString2);
//                    sampleDataList.add(new Entry(msgInt, ++lineChartCounter));
//                    System.out.println("***** DataList: " + sampleDataList + " *****");
//                    addEntry(msgInt);
                } catch (NumberFormatException nfe) {
                    System.out.println("***** message cannot be parsed into int: "+ msgString2 +" *****");
                }

            } catch (Exception e) {
                System.out.println("***** Unable to convert message bytes to string: "+ msgString2 +" *****");
            }
        }
    }

    public static String bytesToDecimal(byte[] bytes) {
        char[] decimalChars = new char[bytes.length * 4];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            decimalChars[j * 4] = decimalArray[v / 100];
            decimalChars[j * 4 + 1] = decimalArray[(v / 10) % 10];
            decimalChars[j * 4 + 2] = decimalArray[v % 10];
            decimalChars[j * 4 + 3] = ' ';
        }
        return new String(decimalChars);
    }

    private void sendBluetoothData(String data) {
        // check connection
        if (!connected) {
            System.out.println("***** Failed to send data due to no connection. *****");
            showToastMessage("Please connect to bluetooth");
        } else {
            // find command characteristics from the GATT server
            BluetoothGattCharacteristic cmdCharacteristic = BluetoothUtils.findCommandCharacteristic(bluetoothGatt);
            // disconnect if the characteristic is not found
            if (cmdCharacteristic == null) {
                System.out.println("***** Unable to find cmd characteristic *****");
                disconnectGattServer();
                return;
            }
            // start stimulation
            startStimulation(cmdCharacteristic, 1, data);
        }
    }

    private void startStimulation(BluetoothGattCharacteristic cmdCharacteristic, final int programId, String data) {
        // set values to the characteristic
        cmdCharacteristic.setValue(data);
        // write the characteristic
        boolean writeSuccess = bluetoothGatt.writeCharacteristic(cmdCharacteristic);
        // check the result
        if (writeSuccess) {
            System.out.println("***** Wrote: "+ data +" *****");
        } else {
            System.out.println("***** Failed to write command *****");
        }
    }

    // Disconnect Gatt Server
    public void disconnectGattServer() {
        System.out.println("***** Closing Gatt connection *****");
        // reset the connection flag
        connected = false;
        // making sure to set characteristic notification to false
        if (bluetoothGatt.getDevice().getUuids() != null) {
            initialized = bluetoothGatt.setCharacteristicNotification(characteristic, false);
        }
        // disconnect and close the gatt
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    private void drawChart() {
        //// Draw Chart
        //// https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/main/java/com/xxmassdeveloper/mpchartexample/CombinedChartActivity.java
        lineChart = findViewById(R.id.chart);
        lineChart.setOnChartValueSelectedListener(this);
        // no description text
        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);
        // enable touch gestures
        lineChart.setTouchEnabled(true);
        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(true);
        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);
        lineChart.setDoubleTapToZoomEnabled(true);
        lineChart.setAutoScaleMinMaxEnabled(true);
        lineChart.setBackgroundColor(getResources().getColor(R.color.colorWhiteFDFD,getTheme()));

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        // add empty data
        lineChart.setData(data);

        // get the legend (only possible after setting data)
        Legend legend = lineChart.getLegend();
        // modify the legend ...
//         legend.setPosition(Legend.LegendPosition.LEFT_OF_CHART);
        legend.setForm(Legend.LegendForm.LINE);
//        Typeface tf = Typeface.createFromAsset(getApplicationContext().getAssets(), "");
//        legend.setTypeface(tf); // font type
        legend.setTextColor(getResources().getColor(R.color.colorBlack2D2D, getTheme()));
        LegendEntry legendEntrySample = new LegendEntry();
        LegendEntry legendEntryControl = new LegendEntry();
        legendEntrySample.label = "Sample";
        legendEntrySample.formColor = getResources().getColor(R.color.colorPrimaryOrange,getTheme());
        legendEntryControl.label = "Control";
        legendEntryControl.formColor = getResources().getColor(R.color.colorPrimaryBlue,getTheme());
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setWordWrapEnabled(true);
        legend.setDrawInside(true);
        legend.setYOffset(10f);
        legend.setCustom(Arrays.asList(legendEntrySample, legendEntryControl));

        XAxis xAxis = lineChart.getXAxis();
//        xl.setTypeface(tf);
        xAxis.setDrawGridLines(true);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisMinimum(0f); // this replaces setStartAtZero(true)
        xAxis.setDrawAxisLine(true);
        xAxis.setTextColor(getResources().getColor(R.color.colorBlack2D2D,getTheme()));
        xAxis.setEnabled(true);

        YAxis yAxisLeft = lineChart.getAxisLeft();
//        leftAxis.setTypeface(tf);
        yAxisLeft.setTextColor(getResources().getColor(R.color.colorBlack2D2D,getTheme()));
        yAxisLeft.setDrawLabels(true);
        yAxisLeft.setDrawAxisLine(true);
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setSpaceTop(10f);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);

    }

    // Add entry for the chart
    // https://github.com/crashlytics/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/RealtimeLineChartActivity.java
    private void addEntry(int receivedData) {
        LineData data = lineChart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(0);
            // set.addEntry(...); // can be called as well
            if (set == null) {
                set = createSet();
                data.addDataSet(set);
            }
            // add a new x-value first
            data.addEntry(new Entry(set.getEntryCount(), receivedData), 0);
            // let the chart know it's data has changed
            lineChart.notifyDataSetChanged();
            // limit the number of visible entries
            lineChart.setVisibleXRangeMaximum(120);
//            lineChart.setVisibleYRange(0, 100, YAxis.AxisDependency.LEFT);
            // move to the latest entry
            lineChart.moveViewToX(data.getDataSetCount());
            // this automatically refreshes the chart (calls invalidate())
            // lineChart.moveViewTo(data.getXValCount()-7, 55f, AxisDependency.LEFT);
        }
    }

    private LineDataSet createSet() {
        LineDataSet lineDataSet = new LineDataSet(null, "Dynamic Data");
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setFillAlpha(65);
        lineDataSet.setHighLightColor(Color.rgb(244, 117, 117));
        lineDataSet.setValueTextColor(Color.BLACK);
        lineDataSet.setValueTextSize(9f);
        lineDataSet.setLineWidth(2);
        lineDataSet.setCircleRadius(6);
        lineDataSet.setCircleColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setCircleColorHole(Color.BLUE);
        lineDataSet.setColor(Color.parseColor("#FFA1B4DC"));
        lineDataSet.setDrawCircleHole(true);
        lineDataSet.setDrawCircles(true);
        lineDataSet.setDrawHorizontalHighlightIndicator(false);
        lineDataSet.setDrawHighlightIndicators(false);
        lineDataSet.setDrawValues(false);
        return lineDataSet;
    }

    private String createCurrentTimeName(String fileType) {
        // Unique file name
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMHH_hhmmss");
        Date now = new Date();
        String fileName = simpleDateFormat.format(now) + fileType;
        return fileName;
    }

    private void uploadChartImage(String fileName) {
        final ProgressBar progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.VISIBLE);

        // convert bitmap to jpeg
        Bitmap bitmap = lineChart.getChartBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] bitmapData = baos.toByteArray();

        // storage
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();

        // storage address and file
        final StorageReference storageReferenceChart = firebaseStorage.getReferenceFromUrl("gs://uiuc-designer-dna-net.appspot.com").child("data/"+fileName);
        UploadTask uploadTask = storageReferenceChart.putBytes(bitmapData);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("***** Upload Failed *****");
                progressBar.setVisibility(View.GONE);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("***** Upload Successful *****");
                storageReferenceChart.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        Uri downloadUrl = uri;
                        System.out.println("***** Download Url: " + downloadUrl + " *****");
                    }
                });
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void createTextFile(String fileName, List<?> fileData) {
        // Ask permissions
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.FOREGROUND_SERVICE
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this,PERMISSIONS, PERMISSION_ALL);
        }
        progressBar.setVisibility(View.VISIBLE);
        try {
            File root = new File(this.getFilesDir(), "uiuc-designer-dna-net");
            if (!root.exists()) {
                boolean createDirectoryResult = root.mkdirs();
                if (!createDirectoryResult) {
                    System.out.println("***** create directory failed *****");
                } else {
                    System.out.println("***** Directory was created successfully *****");
                }
            } else {
                System.out.println("***** Directory already exist *****");
            }
            File file = new File(root, fileName);
            FileOutputStream fos = new FileOutputStream(file, true);
            fos.write(fileData.toString().getBytes());
            fos.flush();
            fos.close();
//            FileWriter writer = new FileWriter(file);
            System.out.println("***** filedata.tostring 1 : " + fileData.toString() + " *****");
//            writer.append(fileData.toString());
//            writer.flush();
//            writer.close();
            System.out.println("***** filedata.tostring 2 : " + fileData.toString() + " *****");
            System.out.println("***** file.tostring" + file.toString() + " *****");
            uploadTxtFile(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteTextFile(String fileName) {
        File root = new File(this.getFilesDir() + "/uiuc-designer-dna-net");
        File file = new File(root, fileName);
        boolean deleteStatus = file.delete();
        if (!deleteStatus) {
            System.out.println("***** delete file failed *****");
        } else {
            System.out.println("***** delete file successful *****");
        }
    }

    private void uploadTxtFile(final String fileName) {
        // storage
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        // storage address and file
        final StorageReference storageReferenceTxt = firebaseStorage.getReferenceFromUrl("gs://uiuc-designer-dna-net.appspot.com").child("data/"+fileName);
        String filePath = this.getFilesDir()+"/uiuc-designer-dna-net/"+fileName;
        System.out.println("***** file path: "+ filePath +" *****");
        Uri fileUri = Uri.fromFile(new File(filePath));
        UploadTask uploadTask = storageReferenceTxt.putFile(fileUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("***** Upload Failed *****");
                progressBar.setVisibility(View.GONE);
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("***** Upload Successful *****");
                storageReferenceTxt.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        System.out.println("***** Download Url: " + uri + " *****");
                    }
                });
                progressBar.setVisibility(View.GONE);
                deleteTextFile(fileName);
            }
        });
    }

    private void downloadFileForScanBeaconTimeStamp(String fileName, final String positivePatientUUID) {
        System.out.println("***** downloadfileforscanbeacontimestamp *****");
        // storage
        FirebaseStorage firebaseStorage = FirebaseStorage.getInstance();
        // Create a storage reference from our app
//        StorageReference storageRef = firebaseStorage.getReference();
        // Create a reference with an initial file path and name
//        StorageReference pathReference = storageRef.child("data/"+fileName);
        // Create a reference to a file from a Google Cloud Storage URI
        StorageReference storageReference = firebaseStorage.getReferenceFromUrl("gs://uiuc-designer-dna-net.appspot.com").child("data/"+fileName);

        final long ONE_MEGABYTE = 1024 * 1024;
        storageReference.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data is returned
                beaconTimeStampData = new String(bytes, StandardCharsets.UTF_8);
                System.out.println("***** downloaded data: " + beaconTimeStampData + " *****");
                scanBeaconTimeStamp(positivePatientUUID);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("***** download failed : "+ e);
            }
        });
    }

    private void showToastMessage(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    private void createNotificationRecentPositive() {

        System.out.println(" ***** create notification channel");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_main);
        builder.setContentTitle("You have been exposed!");
        builder.setContentText("You have been exposed to a recently tested positive COVID19 patient. Please perform a new COVID19 test with Designer DNA Net instrument.");
        builder.setColor(Color.RED);
        // 사용자가 탭을 클릭하면 자동 제거
        builder.setAutoCancel(true);
        // 알림 표시
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "기본 채널", NotificationManager.IMPORTANCE_HIGH));
        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationWithinRange() {
        System.out.println(" ***** create notification channel");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_main)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_main))
                .setContentTitle("You are being exposed!")
                .setContentText("COVID19 Patient is within your range!")
                .setColor(getResources().getColor(R.color.colorOrangeF79256))
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        // 알림 표시
        notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "기본 채널", NotificationManager.IMPORTANCE_HIGH));
        // id값은
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, builder.build());
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

    private void showNotification(Notification notification) {
        // 정의해야하는 각 알림의 고유한 int값
        notificationManager.notify(1, notification);
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}