package com.example.ljdgg.app1;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothMgr {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private ScanCallback mScanCallback;
    private Handler mHandler;
    private Handler eventCheckHandler;
    private Context cont;
    private Handler contextMsgHandler;

    public static final int DISCONNECTED = 0;
    public static final int SCANNING = 1;
    public static final int KEYFOUND = 2;
    public static final int DISCOVERING = 3;
    public static final int DISCOVERED = 4;
    public int connectionStatus = 0;
    public boolean isCharging = false;
    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
    }

    public BluetoothMgr(Context cont, BluetoothAdapter bAdapter, Handler d) {
        this.cont = cont;
        mHandler = new Handler();
        if(d != null){
            contextMsgHandler = d;
        }
        eventCheckHandler = new Handler();
        eventCheckHandler.postDelayed(delayedCheck, 1000);
        mBluetoothAdapter = bAdapter;
        setBluetoothAdapter(mBluetoothAdapter);


    }


    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        ScanFilter.Builder builder = new ScanFilter.Builder();
        // builder.setServiceUuid(Constants.Service_UUID);
        scanFilters.add(builder.build());
        return scanFilters;
    }


    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    public void startScanning() {
        if(connectionStatus == DISCOVERED || connectionStatus == DISCOVERING){
            myLog("not dc'ed, cant start scanning");
            return;
        }
        if (mScanCallback == null) {
            found = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, 3000);

            mScanCallback = new SampleScanCallback();
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
            connectionStatus = SCANNING;
            myLog("\nstart scanning");
        } else {
            myLog("\nalready scanning");
        }
    }
    private int scanTimeout = 0;
    Runnable delayedCheck = new Runnable() {
        @Override
        public void run() {
//            if (connectionStatus != DISCONNECTED && connectionStatus != DISCOVERED && mScanCallback == null && scanTimeout>15) {
//                scanTimeout = 0;
//                connectionStatus = DISCONNECTED;
//            }
//            else if (connectionStatus == DISCONNECTED || connectionStatus == DISCOVERED) {
//                scanTimeout = 0;
//            }else {
//                scanTimeout++;
//            }
            if (!found && mScanCallback == null) {
                connectionStatus = DISCONNECTED;
            }
//            if (bGatt != null && connectionStatus == DISCOVERED) {
//                bGatt.readCharacteristic(bGatt.getService(LEDService_UUID).getCharacteristic(Charge_UUID));
//            }
            Message message = contextMsgHandler.obtainMessage();
            message.what = connectionStatus;
            message.arg1 = isCharging?1:0;
            contextMsgHandler.sendMessage(message);
            //myLog(">>>>>snedMessage"+message.toString());
            eventCheckHandler.postDelayed(this, 1000);
        }
    };

    public void stopScanning() {
        myLog("\nstop scanning");
        mBluetoothLeScanner.stopScan(mScanCallback);
        mScanCallback = null;
    }


    private class SampleScanCallback extends ScanCallback {

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            for (ScanResult result : results) {
                checkResult(result);
            }

        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            checkResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            // Toast.makeText(getActivity(), "Scan failed with error: " + errorCode, Toast.LENGTH_LONG).show();
        }
    }

    boolean found = false;
    BluetoothGatt bGatt;


    protected void onDestroy() {
        eventCheckHandler.removeCallbacks(delayedCheck);
        if (bGatt == null) {
            return;
        }
        bGatt.close();
        bGatt = null;
    }

    public void myLog(String s) {
        Log.i(">>>>>>>>>", s);

    }
    UUID LEDSwitch_UUID;
    UUID R_UUID;
    UUID G_UUID;
    UUID B_UUID;
    UUID Charge_UUID;
    UUID LEDService_UUID;
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            myLog("onConnectionStateChange Status: " + status);
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    myLog("gattCallback STATE_CONNECTED");
                    connectionStatus = DISCOVERING;
                    gatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    myLog("gattCallback STATE_DISCONNECTED");
                    connectionStatus = DISCONNECTED;
                    break;
                default:
                    myLog("gattCallback STATE_OTHER");
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> services = gatt.getServices();
            for (BluetoothGattService s : services) {
                myLog("onServicesDiscovered uuid=" + s.getUuid().toString());
                connectionStatus = DISCOVERED;
                List<BluetoothGattCharacteristic> characteristics = s.getCharacteristics();
                myLog("@@"+characteristics.toString());
                for (BluetoothGattCharacteristic c : characteristics) {
                    if (c.getUuid().toString().equalsIgnoreCase( "A20018A0-6461-4F7F-AC4D-EC1AC6531BC4")) {
                        myLog("Found Wireless Charge Characteristic");
                        Charge_UUID=c.getUuid();
                        LEDService_UUID= s.getUuid();
                        UUID UUID_CCC = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                        BluetoothGattDescriptor config = c.getDescriptor(UUID_CCC);
                        config.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bGatt.writeDescriptor(config);
                        bGatt.setCharacteristicNotification(c, true);
                    } else if (c.getUuid().toString().equalsIgnoreCase( "a20018a0-6461-4f7f-ac4d-ec1ac6531bd0")) {
                        myLog("Found LED Switch Characteristic");
                        LEDSwitch_UUID=c.getUuid();
                        LEDService_UUID=s.getUuid();
                    } else if (c.getUuid().toString().equalsIgnoreCase( "a20018a0-6461-4f7f-ac4d-ec1ac6531bd1")) {
                        myLog("Found R Characteristic");
                        R_UUID=c.getUuid();
                        LEDService_UUID=s.getUuid();
                    }   else if (c.getUuid().toString().equalsIgnoreCase( "a20018a0-6461-4f7f-ac4d-ec1ac6531bd2")) {
                        myLog("Found G Characteristic");
                        G_UUID=c.getUuid();
                        LEDService_UUID=s.getUuid();
                    }else if (c.getUuid().toString().equalsIgnoreCase( "a20018a0-6461-4f7f-ac4d-ec1ac6531bd3")) {
                        myLog("Found B Characteristic");
                        B_UUID=c.getUuid();
                        LEDService_UUID=s.getUuid();
                    }
                    myLog(c.getUuid().toString());
//                    gatt.readCharacteristic(c);
//                    try{
//                        Thread.sleep(1000);
//                    }
//                    catch(InterruptedException e){
//                        myLog("InterruptedExcepteion Catched");
//                    }
                }

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic
                                                 characteristic, int status) {
            myLog("onCharacteristicRead "+ characteristic.toString());
            if (characteristic.getUuid() == Charge_UUID) {
                myLog("cc"+characteristic.getValue().toString());
            }
            //gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged( BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid() == Charge_UUID) {
                int chargeStateRaw = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0);
                //myLog(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,0).toString());
                isCharging = chargeStateRaw<3;
            }
        }

        };


    public void checkResult(ScanResult result) {
        if (found) {
            return;
        }
//        t1.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        t1.append("\n>>>name:\t" + result.getDevice().getName());
//        t1.append("\n>>>rssi:\t" + result.getRssi() + "dBm");
//        t1.append("\n>>>addr:\t" + result.getDevice().getAddress());
//        t1.append("\n>>>class:\t" + result.getDevice().getBluetoothClass());
//        t1.append("\n>>>type:\t" + result.getDevice().getType());
//        t1.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        if (result.getDevice().getName()!= null && result.getDevice().getName().startsWith("UAES")) {
            myLog("\nKey found, connecting...");
            connectionStatus = KEYFOUND;
            found = true;
            bGatt = result.getDevice().connectGatt(cont, true, gattCallback);

        }
    }



    public boolean lightOn() {
        if(connectionStatus!=DISCOVERED){return false;}
        if (bGatt == null) {
            return false;
        }
        BluetoothGattService s = bGatt.getService(LEDService_UUID);
        BluetoothGattCharacteristic c = s.getCharacteristic(LEDSwitch_UUID);
        byte[] on = {0x2};
        byte[] off = {0x00};
        c.setValue(on);
        boolean b = bGatt.writeCharacteristic(c);
        if(b){
            myLog("write success");
        }else{
            myLog("write failed");
        }
        return b;
    }
    public boolean lightOff() {
        if(connectionStatus!=DISCOVERED){return false;}
        if (bGatt == null) {
            return false;
        }
        BluetoothGattService s = bGatt.getService(LEDService_UUID);
        BluetoothGattCharacteristic c = s.getCharacteristic(LEDSwitch_UUID);
        byte[] on = {0x2};
        byte[] off = {0x0};
        c.setValue(off);
        boolean b = bGatt.writeCharacteristic(c);
        if(b){
            myLog("write success");
        }else{
            myLog("write failed");
        }
        return b;
    }
    public boolean WriteR(int val) {
        if(connectionStatus!=DISCOVERED){return false;}
        if (bGatt == null) {
            return false;
        }
        BluetoothGattService s = bGatt.getService(LEDService_UUID);
        BluetoothGattCharacteristic c = s.getCharacteristic(R_UUID);
        byte[] perc = {(byte)val};
        c.setValue(perc);
        boolean b = bGatt.writeCharacteristic(c);
        if(b){
            myLog("write success");
        }else{
            myLog("write failed");
        }
        return b;
    }

    public boolean WriteG(int val) {
        if(connectionStatus!=DISCOVERED){return false;}
        if (bGatt == null) {
            return false;
        }
        BluetoothGattService s = bGatt.getService(LEDService_UUID);
        BluetoothGattCharacteristic c = s.getCharacteristic(G_UUID);
        byte[] perc = {(byte)val};
        c.setValue(perc);
        boolean b = bGatt.writeCharacteristic(c);
        if(b){
            myLog("write success");
        }else{
            myLog("write failed");
        }
        return b;
    }

    public boolean WriteB(int val) {
        if(connectionStatus!=DISCOVERED){return false;}
        if (bGatt == null) {
            return false;
        }
        BluetoothGattService s = bGatt.getService(LEDService_UUID);
        BluetoothGattCharacteristic c = s.getCharacteristic(B_UUID);
        byte[] perc = {(byte)val};
        c.setValue(perc);
        boolean b = bGatt.writeCharacteristic(c);
        if(b){
            myLog("write success");
        }else{
            myLog("write failed");
        }
        return b;
    }

}


