package com.example.designerdnanet;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.example.designerdnanet.Constants.MAC_ADDRESS;
import static com.example.designerdnanet.Constants.MAC_ADDRESS_BLUNO;
import static com.example.designerdnanet.Constants.REQUEST_ENABLE_BT;
import static com.example.designerdnanet.Constants.REQUEST_FINE_LOCATION;
import static com.example.designerdnanet.Constants.SCAN_PERIOD;
import static com.example.designerdnanet.Constants.UUID_CTRL_COMMAND;
import static com.example.designerdnanet.Constants.UUID_TDCS_SERVICE;
import static com.example.designerdnanet.MainActivity.bytesToDecimal;

public class ScanDeviceActivity extends AppCompatActivity {
    private BluetoothAdapter bluetoothAdapter;
    private DeviceListAdapter deviceListAdapter;
    private boolean isScanning = false;
    private Handler handler;
    private List<Object> deviceList;
    private String scannedDeviceAddress;
    // scan results
    private Map<String, BluetoothDevice> scanResults;
    // scan callback
    private ScanCallback scanCallback;
    // ble scanner
    private BluetoothLeScanner bleScanner;
    // connected device;
    private BluetoothDevice connectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_device);
        final Button backButton = findViewById(R.id.buttonBack);

        deviceList = new ArrayList<>();
        handler = new Handler();

        System.out.println("&&&&& oncreate "+ scanResults);

        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_spported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter. For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    protected void onResume() {
        super.onResume();
        // Initializes list view adapter.

        RecyclerView recyclerView = findViewById(R.id.recyclerViewDevice);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        deviceListAdapter = new DeviceListAdapter(this, deviceList);
        recyclerView.setAdapter(deviceListAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        System.out.println(" &&&&& onresume");
        scanDevice(true);
        try {
            BluetoothDevice device = getIntent().getExtras().getParcelable("connectedDevice");
            System.out.println("&&&&& connected device from intent "+ device);
            connectedDevice = device;
            if (connectedDevice!= null) {
                addScanResult(connectedDevice);
                System.out.println("&&&&& connected device "+ connectedDevice);
            }
        } catch (Exception e) {
            System.out.println("***** No connected device *****");
        }
    }

    private void requestEnableBLE() {
        Intent enableBleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBleIntent, REQUEST_ENABLE_BT);
    }

    private void requestLocationPermission() {
        requestPermissions( new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanDevice(false);
    }

    private void addScanResult(BluetoothDevice bluetoothDevice) {
        System.out.println("&&&&& add scan result");
        System.out.println("&&&&& add scan result. bluetoothdevice " + bluetoothDevice);
        System.out.println("&&&&& add scan result. bluetoothdevice get name " + bluetoothDevice.getName());
        System.out.println("&&&&& add scan result. bluetoothdevice get address" + bluetoothDevice.getAddress());

        String deviceName = bluetoothDevice.getName();
        String deviceHardwareAddress = bluetoothDevice.getAddress();
        String connectedDeviceAddress = deviceHardwareAddress;
        DeviceInfoModel deviceInfoModel = new DeviceInfoModel(bluetoothDevice, deviceName,connectedDeviceAddress);
        System.out.println("&&&&& add device on empty device list");
        deviceList.add(deviceInfoModel);
        if (deviceList.isEmpty()) {
            System.out.println("&&&&& add device on empty device list");
            deviceList.add(deviceInfoModel);
        } else {
            System.out.println("&&&&& add device on not empty device list");

            for(int i = 0; i < deviceList.size(); i++) {
                DeviceInfoModel scannedDevice = (DeviceInfoModel) deviceList.get(i);
                String scannedDeviceAddress = scannedDevice.getDeviceHardwareAddress();
                if (!deviceHardwareAddress.equals(scannedDeviceAddress)) {
                    deviceList.add(deviceInfoModel);
                } else {
                    bleScanner.stopScan(scanCallback);
                }
            }
        }
        deviceListAdapter.notifyDataSetChanged();
    }

    private void scanDevice(final boolean enable) {
        // Check BLE Adapter and BLE enabled
        System.out.println("&&&&& scan device");

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            requestEnableBLE();
            return;
        }

        // Check location Permission
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }

        //// Set scan filters
        // Create scan filter list
        List<ScanFilter> filters = new ArrayList<>();
        // Create a scan filter with device mac address
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceAddress(MAC_ADDRESS).build();
        // Add the filter to the list
        filters.add(scanFilter);

        //// Scan Settings
        // Set low power scan mode
        ScanSettings scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        scanResults = new HashMap<>();
        scanCallback = new BLEScanCallback(scanResults);

        if (enable) {
            System.out.println("&&&&& enable");

            // Stop scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isScanning && bluetoothAdapter != null && bluetoothAdapter.isEnabled() && bleScanner != null) {
                        // stop scanning
                        System.out.println("&&&&& stop scan on run");
                        Set<BluetoothDevice> bondedDevice = bluetoothAdapter.getBondedDevices();
                        System.out.println("&&&&& bonded device " + bondedDevice);
//                        if (!bondedDevice.isEmpty()) {
//                            System.out.println("&&&&& bonded device "+bondedDevice);
//                            deviceList.add(bondedDevice);
//                            deviceListAdapter.notifyDataSetChanged();
//                        }
                        bleScanner.stopScan(scanCallback);
                        scanComplete();
                    }
                    // reset flag
                    scanCallback = null;
                    isScanning = false;
                    handler = null;
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            //// Now ready to scan
            // start scan
            System.out.println("&&&&& start scan");

            bleScanner.startScan(filters, scanSettings, scanCallback);
            // Set scanning flag
            isScanning = true;
        } else {
            isScanning = false;
            bleScanner.stopScan(scanCallback);
        }
        invalidateOptionsMenu();
    }

    private class BLEScanCallback extends ScanCallback {
        private Map<String, BluetoothDevice> callbackScanResult;

        // Constructor
        BLEScanCallback(Map<String, BluetoothDevice> scanResult) {
            callbackScanResult = scanResult;
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            System.out.println("&&&&& on scan result");

            if(result.getDevice()!=null) {
                BluetoothDevice bluetoothDevice = result.getDevice();
                addScanResult(bluetoothDevice);
            } else {
                System.out.println("***** No Bluetooth Device Scanned *****");
            }
            super.onScanResult(callbackType, result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            System.out.println("&&&&& onbatchscanresult ");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            System.out.println("&&&&& onscanfailed ");

        }

        private void addScanResult(BluetoothDevice bluetoothDevice) {
            System.out.println("&&&&& add scan result");

            String deviceName = bluetoothDevice.getName();
            String deviceHardwareAddress = bluetoothDevice.getAddress();
            scannedDeviceAddress = deviceHardwareAddress;
            DeviceInfoModel deviceInfoModel = new DeviceInfoModel(bluetoothDevice, deviceName,deviceHardwareAddress);
            if (deviceList.isEmpty()) {
                deviceList.add(deviceInfoModel);
            } else {
                for(int i = 0; i < deviceList.size(); i++) {
                    DeviceInfoModel scannedDevice = (DeviceInfoModel) deviceList.get(i);
                    String scannedDeviceAddress = scannedDevice.getDeviceHardwareAddress();
                    if (!deviceHardwareAddress.equals(scannedDeviceAddress)) {
                        deviceList.add(deviceInfoModel);
                    } else {
                        bleScanner.stopScan(scanCallback);
                    }
                }
            }
            deviceListAdapter.notifyDataSetChanged();
            callbackScanResult.put(deviceHardwareAddress, bluetoothDevice);

        }
    }

    private void scanComplete() {
        // check if nothing found
        if (scanResults.isEmpty()) {
            System.out.println("***** Scan result is empty *****");
            return;
        }

        // loop over the scan results and connect to them
        for (String deviceAddress : scanResults.keySet()) {
            System.out.println("***** Found device: " + deviceAddress);
            // get device instance using its MAC Address
            BluetoothDevice bluetoothDevice = scanResults.get(deviceAddress);
            //connectDevice(bluetoothDevice);
        }
    }

}