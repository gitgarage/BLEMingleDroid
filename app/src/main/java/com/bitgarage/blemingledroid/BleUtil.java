package com.bitgarage.blemingledroid;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;

/**
 * Util for Bluetooth Low Energy
 */
public class BleUtil {

    private BleUtil() {
        // Util
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

    public static AdvertiseSettings createAdvSettings(boolean connectable, int timeoutMillis) {
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        builder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        builder.setConnectable(connectable);
        builder.setTimeout(timeoutMillis);
        builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return builder.build();
    }

    public static AdvertiseData makeAdvertiseData(String message) {
        while (message.length() < 9)
        {
            message += " ";
        }
        byte[] data = message.substring(2).getBytes();
        ParcelUuid pu = ParcelUuid.fromString("0000" + asHex(message.substring(0,2).getBytes()) + "-0000-1000-8000-00805F9B34FB");
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        builder.addServiceData(pu, data);
        builder.addServiceUuid(pu);

        return builder.build();
    }

    /** check if BLE Supported device */
    public static boolean isBLESupported(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /** get BluetoothManager */
    public static BluetoothManager getManager(Context context) {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }
}
