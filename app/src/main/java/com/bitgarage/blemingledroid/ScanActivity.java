package com.bitgarage.blemingledroid;

import java.security.Permissions;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter mBTAdapter;
    private BluetoothLeAdvertiser mBTAdvertiser;
    public boolean CONNECTED = false;
    private boolean mIsScanning;
    private Button mSendButton;
    private EditText mEditText;
    private String TAG = "ScanActivity";
    private Handler threadHaandler = new Handler();

    private AdvertiseCallback mAdvCallback = new AdvertiseCallback() {
        public void onStartSuccess(android.bluetooth.le.AdvertiseSettings settingsInEffect) {
            CONNECTED = true;
            if (settingsInEffect != null) {
            } else {
                Log.d(TAG, "onStartSuccess, settingInEffect is null");
            }
            setProgressBarIndeterminateVisibility(false);
        }

        public void onStartFailure(int errorCode) {
            CONNECTED = false;
        };
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;

        requestPermissions(
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);


        init();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if ((mBTAdapter != null) && (!mBTAdapter.isEnabled())) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

//        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public static String asHex(byte bytes[]) {
        if ((bytes == null) || (bytes.length == 0)) {
            return "";
        }

        StringBuffer sb = new StringBuffer(bytes.length * 2);

        for (int index = 0; index < bytes.length; index++) {
            int bt = bytes[index] & 0xff;

            if (bt < 0x10) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(bt).toUpperCase());
        }
        return sb.toString();
    }

    public String[] used = new String[3];
    public int ui = 0;

    @Override
    public void onLeScan(final BluetoothDevice newDevice, final int newRssi,
                         final byte[] newScanRecord) {

        int startByte = 0;
        String hex = asHex(newScanRecord).substring(0,29);
        while (startByte <= 5) {
            if (!Arrays.asList(used).contains(hex)) {
                used[ui] = hex;

                String message = new String(newScanRecord);
                Log.e("String", message);
                String firstChar = message.substring(5, 6);
                Pattern pattern = Pattern.compile("[ a-zA-Z0-9~!@#$%^&*()_+{}|:\"<>?`\\-=;',\\./\\[\\]\\\\]", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(firstChar);
                if (firstChar.equals("L"))
                {
                    firstChar = message.substring(6, 7);
                    pattern = Pattern.compile("[ a-zA-Z0-9~!@#$%^&*()_+{}|:\"<>?`\\-=;',\\./\\[\\]\\\\]", Pattern.DOTALL);
                    matcher = pattern.matcher(firstChar);
                }

                if(matcher.matches())
                {
                    TextView textViewToChange = (TextView) findViewById(R.id.textView);
                    String oldText = textViewToChange.getText().toString();
                    int len = 0;
                    String subMessage = "";
                    while (matcher.matches())
                    {
                        subMessage = message.substring(5, 6+len);
                        matcher = pattern.matcher(message.substring(5+len, 6+len));
                        len++;
                    }
                    subMessage = subMessage.substring(0,subMessage.length()-1);

                    Log.e("Address",newDevice.getAddress());
                    Log.e("Data",asHex(newScanRecord));
                    boolean enter = subMessage.length() == 16;
                    enter = enter && !subMessage.substring(15).equals("-");
                    enter = enter || subMessage.length() < 16;
                    textViewToChange.setText(oldText + subMessage.substring(0, subMessage.length() - 1) + (enter ? "\n" : ""));

                    ui = ui == 2 ? -1 : ui;
                    ui++;

                    Log.e("String", subMessage);
                }
                break;
            }
            startByte++;
        }
    }

    private void sendMessage() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (mBTAdapter == null) {
                    return;
                }
                if (mBTAdvertiser == null) {
                    mBTAdvertiser = mBTAdapter.getBluetoothLeAdvertiser();
                }
                String textMessage = mEditText.getText().toString();
                if (textMessage.length() > 0)
                {
                    String message = "Android: " + textMessage;

                    while (message.length() > 0) {
                        String subMessage;
                        if(message.length() > 8)
                        {
                            subMessage = message.substring(0,8) + "-";
                            message = message.substring(8);
                            for (int i = 0; i < 20; i++)
                            {
                                AdvertiseData ad = BleUtil.makeAdvertiseData(subMessage);
                                mBTAdvertiser.startAdvertising(BleUtil.createAdvSettings(true, 100), ad, mAdvCallback);
                                mBTAdvertiser.stopAdvertising(mAdvCallback);
                            }
                        }
                        else
                        {
                            subMessage = message;
                            message = "";
                            for (int i = 0; i < 5; i++)
                            {
                                AdvertiseData ad = BleUtil.makeAdvertiseData(subMessage);
                                mBTAdvertiser.startAdvertising(
                                        BleUtil.createAdvSettings(true, 40), ad,
                                        mAdvCallback);
                                mBTAdvertiser.stopAdvertising(mAdvCallback);
                            }
                        }
                    }
                    threadHaandler.post(updateRunnable);
                }
            }
        });
        thread.start();
    }

    final Runnable updateRunnable = new Runnable() {
        public void run() {
            mEditText.setText("");
        }
    };

    private void init() {
        mSendButton = (Button) findViewById(R.id.send_button);
        mSendButton.setEnabled(true);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        mEditText = (EditText) findViewById(R.id.editText);

        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        stopScan();
        startScan();
    }

    private void startScan() {
        Log.e("String", "She started it.");

        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        Log.e("String", "I finished it.");

        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }
}