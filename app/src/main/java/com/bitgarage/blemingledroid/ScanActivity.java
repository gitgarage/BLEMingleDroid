package com.bitgarage.blemingledroid;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter mBTAdapter;
    private boolean mIsScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

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

        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setEnabled(true);
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        if ((mBTAdapter == null) || (!mBTAdapter.isEnabled())) {
            menu.findItem(R.id.action_scan).setEnabled(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // ignore
            return true;
        } else if (itemId == R.id.action_scan) {
            startScan();
            return true;
        } else if (itemId == R.id.action_stop) {
            stopScan();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
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

    public String[] used = new String[100];
    public int ui = 0;

    @Override
    public void onLeScan(final BluetoothDevice newDevice, final int newRssi,
                         final byte[] newScanRecord) {

        int startByte = 0;
        String hex = asHex(newScanRecord).substring(0,29);
        while (startByte <= 5) {
            if (!Arrays.asList(used).contains(hex)) {
                used[ui] = hex;
                ui++;

                String message = new String(newScanRecord);
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
                    textViewToChange.setText(oldText + subMessage.substring(0,subMessage.length()-1) + (enter ? "\n" : ""));
                    Log.e("String", subMessage);
                }
                break;
            }
            startByte++;
        }
    }

    private void init() {

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
    }

    private void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }
}