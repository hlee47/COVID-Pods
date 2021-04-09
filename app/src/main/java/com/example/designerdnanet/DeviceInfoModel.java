package com.example.designerdnanet;

import android.bluetooth.BluetoothDevice;

public class DeviceInfoModel {

    private BluetoothDevice bluetoothDevice;
    private String deviceName, deviceHardwareAddress;

    public DeviceInfoModel(){}

    public DeviceInfoModel(BluetoothDevice bluetoothDevice, String deviceName, String deviceHardwareAddress){
        this.deviceName = deviceName;
        this.deviceHardwareAddress = deviceHardwareAddress;
        this.bluetoothDevice = bluetoothDevice;
    }

    public String getDeviceName(){return deviceName;}

    public String getDeviceHardwareAddress(){return deviceHardwareAddress;}

    public BluetoothDevice getBluetoothDevice(){return bluetoothDevice;}
}