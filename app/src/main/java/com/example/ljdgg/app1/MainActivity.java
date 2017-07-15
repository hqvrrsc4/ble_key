package com.example.ljdgg.app1;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;

import android.widget.PopupWindow;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    public static final int DISCONNECTED = 0;
    public static final int SCANNING = 1;
    public static final int KEYFOUND = 2;
    public static final int DISCOVERING = 3;
    public static final int DISCOVERED = 4;
    public static int UIState = 0;
    private Handler eventCheckHandler;

    BluetoothMgr btMgr;
    Button b1;
    TextView t1;
    TextView textViewChargeStatus;
    ProgressBar progressKeyBattery;

    ProgressBar progressBar5;

    public Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            //myLog(">>>>>>> handleMessage"+msg.toString());
            ChangeUIState(msg.what, msg.arg1);
        }

    };
    private BluetoothAdapter mBluetoothAdapter;

    public void setBluetoothAdapter(BluetoothAdapter btAdapter) {
        this.mBluetoothAdapter = btAdapter;
        btMgr = new BluetoothMgr(this, btAdapter, msgHandler);
    }

    boolean LED_Enabled = false;
    boolean LED_EnabledChanged = false;
    int LED_RedValue = 0;
    int LED_GreenValue = 0;
    int LED_BlueValue = 0;
    boolean LED_RedFlagChanged = false;
    boolean LED_GreenFlagChanged = false;
    boolean LED_BlueFlagChanged = false;
    Switch RGBSwitch;
    SeekBar seekBarRed;
    SeekBar seekBarGreen;
    SeekBar seekBarBlue;
    TextView textViewRed;
    TextView textViewGreen;
    TextView textViewBlue;
    private void initRGBControl(){
        RGBSwitch = (Switch) findViewById(R.id.switch1);
        seekBarRed = (SeekBar) findViewById(R.id.seekBar4);
        seekBarGreen = (SeekBar) findViewById(R.id.seekBar5);
        seekBarBlue = (SeekBar) findViewById(R.id.seekBar6);
        textViewRed = (TextView) findViewById(R.id.textView13);
        textViewGreen = (TextView) findViewById(R.id.textView15);
        textViewBlue = (TextView) findViewById(R.id.textView17);
        RGBSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                LED_Enabled=b;
                LED_EnabledChanged=true;
                //syncRGBControl();
            }
        });
        seekBarRed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                LED_RedValue = i;
                LED_RedFlagChanged = i>0;
                //syncRGBControl();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarRed.setMax(100);

        seekBarGreen.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                LED_GreenValue = i;
                LED_GreenFlagChanged = i>0;
                //syncRGBControl();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarGreen.setMax(100);

        seekBarBlue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                LED_BlueValue = i;
                LED_BlueFlagChanged = i>0;
                //syncRGBControl();
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarBlue.setMax(100);

    }
    private void resetRGBControl(){
        RGBSwitch.setChecked(false);
        seekBarRed.setProgress(0);
        seekBarGreen.setProgress(0);
        seekBarBlue.setProgress(0);
        textViewRed.setText("0");
        textViewGreen.setText("0");
        textViewBlue.setText("0");
    }
    private void syncRGBControl(){
        RGBSwitch.setChecked(LED_Enabled);
        seekBarRed.setProgress(LED_RedValue);
        seekBarGreen.setProgress(LED_GreenValue);
        seekBarBlue.setProgress(LED_BlueValue);
        textViewRed.setText(LED_RedValue+"");
        textViewGreen.setText(LED_GreenValue+"");
        textViewBlue.setText(LED_BlueValue+"");
    }
    private void syncRemoteLED(){
        if (LED_Enabled && UIState == DISCOVERED) {
            if (LED_RedFlagChanged) {
                myLog(LED_RedValue+" (WriteR)");
                boolean writeSuccess = btMgr.WriteR(LED_RedValue);
                LED_RedFlagChanged=!writeSuccess;
            }
            if (LED_GreenFlagChanged) {
                myLog(LED_GreenValue+" (WriteG)");
                boolean writeSuccess = btMgr.WriteG(LED_GreenValue);
                LED_GreenFlagChanged=!writeSuccess;
            }
            if (LED_BlueFlagChanged) {
                myLog(LED_BlueValue+" (WriteB)");
                boolean writeSuccess = btMgr.WriteB(LED_BlueValue);
                LED_BlueFlagChanged=!writeSuccess;
            }
            if (LED_EnabledChanged) {
                boolean writeSuccess = false;

                if(LED_Enabled){
                    myLog("ON (WriteSwitch)");
                    writeSuccess = btMgr.lightOn();
                }else{
                    myLog("OFF (WriteSwitch)");
                    writeSuccess = btMgr.lightOff();
                }
                LED_EnabledChanged=!writeSuccess;
            }
        }else if (!LED_Enabled && UIState == DISCOVERED) {
            if (LED_EnabledChanged) {
                boolean writeSuccess = false;
                myLog("OFF (WriteSwitch)");
                writeSuccess = btMgr.lightOff();
                LED_EnabledChanged=!writeSuccess;
            }
        }
    }

    Runnable delayedCheck = new Runnable() {
        @Override
        public void run() {
            syncRemoteLED();
            eventCheckHandler.postDelayed(this, 1200);
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_1);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
            }
        }
        mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE))
                .getAdapter();
        eventCheckHandler = new Handler();
        eventCheckHandler.postDelayed(delayedCheck, 1000);

        // Is Bluetooth supported on this device?
        if (mBluetoothAdapter != null) {
            // Is Bluetooth turned on?
            if (mBluetoothAdapter.isEnabled()) {
                // Are Bluetooth Advertisements supported on this device?
                if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    // Everything is supported and enabled
                    setBluetoothAdapter(mBluetoothAdapter);
                } else {

                    t1.append("\nBluetooth Advertisements are not supported.");
                }
            } else {
                // Prompt user to turn on Bluetooth (logic continues in onActivityResult()).
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        } else {

            t1.append("\nBluetooth is not supported");
        }

        setControlViewEnabled(false);
        progressBar5 = (ProgressBar) findViewById(R.id.progressBar5);
        progressBar5.setVisibility(View.INVISIBLE);
        b1 = (Button) findViewById(R.id.button3);
        b1.setOnClickListener(new Button.OnClickListener() {//创建监听
            public void onClick(View v) {
                b1.setEnabled(false);
                progressBar5.setVisibility(View.VISIBLE);
                if (UIState == DISCONNECTED) {
                    btMgr.startScanning();
                } else if (UIState == DISCOVERED) {
                    btMgr.onDestroy();
                    btMgr = new BluetoothMgr(MainActivity.this, mBluetoothAdapter, msgHandler);
                }

            }

        });
        t1 = (TextView) findViewById(R.id.textView4);
        textViewChargeStatus = (TextView) findViewById(R.id.textView8);
        progressKeyBattery = (ProgressBar) findViewById(R.id.progressBar4);
        initRGBControl();

    }

    private void setControlViewEnabled(boolean flag){
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayout1);
        for(int i = 0;i<linearLayout.getChildCount();i++) {
            View child = linearLayout.getChildAt(i);
            if (child instanceof ConstraintLayout) {
                for(int j = 0;j<((ConstraintLayout)child).getChildCount();j++) {
                    View childchild = ((ConstraintLayout)child).getChildAt(j);
                    childchild.setEnabled(flag);
                }
            }
        }
    }

    public void ChangeChargingState(boolean connected, boolean isCharging){
        if (!connected) {
            textViewChargeStatus.setText("钥匙电池：未知");
            progressKeyBattery.setProgress(0);
            return;
        }
        if (isCharging) {
            textViewChargeStatus.setText("钥匙正在充电...");
            progressKeyBattery.setProgress(100);
        }else{
            textViewChargeStatus.setText("钥匙电池：未充电");
            progressKeyBattery.setProgress(90);
        }

    }
    public void ChangeUIState(int state, int isCharging) {
        ChangeChargingState(state == DISCOVERED, isCharging == 1);
        if (UIState == DISCONNECTED && state == DISCONNECTED) {
            b1.setEnabled(true);
            progressBar5.setVisibility(View.INVISIBLE);
            setControlViewEnabled(false);
            t1.setText("未连接");
            b1.setText("连接");
        } else if (state == DISCONNECTED) {
            b1.setEnabled(true);
            progressBar5.setVisibility(View.INVISIBLE);
            setControlViewEnabled(false);
            t1.setText("已断开");
            b1.setText("连接");
        } else if (state == SCANNING) {
            b1.setEnabled(false);
            progressBar5.setVisibility(View.VISIBLE);
            setControlViewEnabled(false);
            t1.setText("连接中...");
        } else if (state == KEYFOUND) {
            t1.setText("设备已找到...");
        } else if (state == DISCOVERED) {
            t1.setText("已连接");
            setControlViewEnabled(true);
            b1.setEnabled(true);
            progressBar5.setVisibility(View.INVISIBLE);
            b1.setText("断开");
        }
        UIState = state;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void myLog(String s) {
        Log.i(">>>>>>>>>", s);
    }

    public void b1_click(View view) {
        btMgr.startScanning();
    }

    public void bA_click(View view) {
        btMgr.lightOn();
    }

    public void bB_click(View view) {
        btMgr.lightOff();
    }

    public void bC_click(View view) {
        btMgr.WriteR(20);
    }

    public void b2_click(View view) {
        btMgr.onDestroy();
    }

}
