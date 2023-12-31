/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.navitproject.glassheadup;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

/**
 * Collects and communicates information about the user's current orientation and location.
 */
public class ConnectionManager {

    private static final String TAG = ConnectionManager.class.getSimpleName();
    private final static int MIN_RSSI_VALUE = -70;
    private static final String CONFIGFILE = "headup_serial.txt";
    private static final long SCAN_PERIOD = 5000;
    private final static String NUS_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA8E";
    private final static String NUS_SERVICE_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA8E";
    private final static String NUS_SERVICE_UUID_SERIAL = "6E400004-B5A3-F393-E0A9-E50E24DCCA8E";
    private boolean bonded = false;
    private boolean connected = false;
    private boolean connecting = false;
    private BluetoothGatt mBluetoothGatt = null;
    private byte[] cmdbuffer = new byte[50];
    private int cmdbufferidx = 0;
    private CommandReceiver cmdrec = new CommandReceiver();
    private String deviceserial;
    private String deviceserial_read = "";
    private boolean nusfound = false;
    private BluetoothGattCharacteristic nusrx = null;
    private BluetoothGattCharacteristic nussvcchar = null;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothGattService nusservice = null;

    private Context myContext;

    private Handler mHandler;
    private boolean mScanning;

    /**
     * Initializes a new instance of {@code ConnectionManager}, using the specified context to
     * access system services.
     */
    public ConnectionManager(Context applicationContext, BluetoothManager bluetoothManager) {
        myContext = applicationContext;
        Log.w(TAG, "ConnectionManager() ");
        deviceserial = getDeviceSerialConfigured(myContext);
        Log.e(TAG, "CONFIGURED DEVICESERIAL IS: " + deviceserial);
        mHandler = new Handler();
        bluetoothAdapter = bluetoothManager.getAdapter();
        Set<BluetoothDevice> x = bluetoothManager.getAdapter().getBondedDevices();
        if (x.size() > 0) {
            Log.e(TAG, "Trying bonded device.");
            mBluetoothGatt = x.iterator().next().connectGatt(myContext, false, mGattCallback);
            connecting = true;
            bonded = true;
        } else {
            Log.e(TAG, "Start scanning.");
            ScanThread.start();
        }

    }

    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (rssi < MIN_RSSI_VALUE || !mScanning)
                return;

            if (!connected) {
                if (mBluetoothGatt == null) {
                    connecting = true;
                    mBluetoothGatt = device.connectGatt(myContext, false, mGattCallback);
                    Log.i(TAG, "Found device: " + Utilities.bytesToHex(scanRecord));
                    Log.i(TAG, "refreshDeviceCache: " + refreshDeviceCache(mBluetoothGatt));
                }
            } else {
                Log.w(TAG, "Ignoring device: " + Utilities.bytesToHex(scanRecord) + " as another device is currently connected or connecting.");
            }

        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.d(TAG, "onConnectionStateChange: " + getGATTStatus(status) + ", " + getGATTStatus(newState) + " gatt==mBluetoothGatt: " + (mBluetoothGatt == gatt));

            if (newState == STATE_CONNECTED) {

                Log.e(TAG, "A device connected");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connected = gatt.discoverServices();
                if (connected) {
                    connecting = false;
                    scanLeDevice(false);
                } else {
                    gatt.disconnect();
                    //gatt.close();
                    //mBluetoothGatt = null;
                    //connecting = false;
                }
                Log.e(TAG, "DiscoverServices returned: " + connected);
            }

            if (newState == STATE_DISCONNECTED || newState == 133) { // https://issuetracker.google.com/issues/36976711
                Log.e(TAG, "STATE_DISCONNECTED: " + newState);
                gatt.close();
                mBluetoothGatt = null;
                nusfound = false;
                connected = false;
                connecting = false;
                scanLeDevice(true);
            }
        }

        public String getGATTStatus(int State) {
            switch (State) {
                case BluetoothGatt.GATT_SUCCESS:
                    return "SUCCESS";
                case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                    return "READ_NOT_PERMITTED";
                case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                    return "WRITE_NOT_PERMITTED";
                case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                    return "INSUFFICIENT_AUTHENTICATION";
                case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                    return "REQUEST_NOT_SUPPORTED";
                case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                    return "INSUFFICIENT_ENCRYPTION";
                case BluetoothGatt.GATT_INVALID_OFFSET:
                    return "INVALID_OFFSET";
                case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                    return "INVALID_ATTRIBUTE_LENGTH";
                case BluetoothGatt.GATT_FAILURE:
                    return "FAILURE";
                default:
                    return "Unknown(" + String.format("%02X", State) + ").";
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            super.onServicesDiscovered(gatt, status);
            Log.e(TAG, "onServicesDiscovered " + getGATTStatus(status));

            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.disconnect();
//                gatt.close();
//                mBluetoothGatt = null;
//                connected = false; // state disconnected not signaled sometimes
//                connecting = false;
                //scanLeDevice(true);
                return;
            }

            List<BluetoothGattService> svcs = gatt.getServices();

            for (BluetoothGattService svc : svcs) {
                Log.d(TAG, "Service discovered: " + svc.getUuid().toString());
                if (!nusfound && svc.getUuid().toString().equals(NUS_SERVICE_UUID.toLowerCase())) {
                    Log.w(TAG, "Found NUS Service: " + svc.getUuid().toString());

                    nusfound = true;
                    nusservice = svc;
                    nussvcchar = nusservice.getCharacteristic(UUID.fromString(NUS_SERVICE_UUID_SERIAL));
                    gatt.readCharacteristic(nussvcchar);
                }
            }

            if (!nusfound) {
                gatt.disconnect();
//                gatt.close();
//                connected = false; // state disconnected not signaled sometimes
//                connecting = false;
                Log.w(TAG, "Disconnected device as service was not found or device serial didn't match");

            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("DEBUG", "onCharacteristicRead called");
                try {
                    if (characteristic.getUuid().equals(UUID.fromString(NUS_SERVICE_UUID_SERIAL))) {
                        deviceserial_read = characteristic.getStringValue(0);

                        if (nussvcchar != null) {
                            if (deviceserial_read.equals(deviceserial)) {
                                Log.e(TAG, "Found the configured device with serial " + deviceserial_read);
                            } else {
                                Log.e(TAG, "Wrong device, wrong serial number! " + deviceserial_read);
                                nusfound = false;
                                nusservice = null;
                                return;
                            }
                        } else {
                            Log.e(TAG, "Wrong device, no serial number characteristic available! ");
                            nusfound = false;
                            nusservice = null;
                            gatt.disconnect();
                            return;
                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }


                        nusrx = nusservice.getCharacteristic(UUID.fromString(NUS_SERVICE_UUID_RX));
                        if (mBluetoothGatt.setCharacteristicNotification(nusrx, true)) {
                            Log.w(TAG, "Subscribed to RX Notifictions successfuly.");
                        } else {
                            Log.e(TAG, "Subscription failed");
                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        nusrx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        BluetoothGattDescriptor descriptor = nusrx.getDescriptors().get(0);
                        boolean b = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        Log.w(TAG, "ENABLE_NOTIFICATION_VALUE written: " + descriptor.getUuid() + " " + descriptor.getPermissions() + " " + b);
                        mBluetoothGatt.writeDescriptor(descriptor);

                    }

                } catch (Exception ignored) {

                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.d("DEBUG", "onCharacteristicWrite called");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            //Log.d("DEBUG", "onCharacteristicChanged called");
            byte x[] = nusrx.getValue();
            //Log.d("DEBUG", "DATA: " + bytesToHex(x));
            for (byte b : x) {

                if (cmdbufferidx < 50)
                    cmdbuffer[cmdbufferidx++] = b;
                else {
                    cmdbufferidx = 0;
                    Log.e("DEBUG", "CMDBUFFERIDX too large!");
                    break;
                }
            }

            if (cmdbufferidx > 1) {
                if (cmdbuffer[cmdbufferidx - 2] == '\r' && cmdbuffer[cmdbufferidx - 1] == '\n') {

                    cmdrec.assembleCommand(cmdbuffer, cmdbufferidx);
                    cmdbufferidx = 0;
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.d("DEBUG", "onDescriptorRead called");
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.d("DEBUG", "onDescriptorWrite called: " + status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
            Log.d("DEBUG", "onReliableWriteCompleted called");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Log.d("DEBUG", "onReadRemoteRssi called");
        }
    };
    Thread ScanThread = new Thread() {
        public void run() {
            while (true) {
                try {
                    if (!mScanning && !connected) {
                        connecting = false;
                        scanLeDevice(true);
                        Log.w("SCANTHREAD", "Starting LE Scan.");
                    } else {
                        Log.w("SCANTHREAD", "Skipping LE Scan - connected: " + connected + " mScanning: " + mScanning);
                    }
                    Thread.sleep(10000);
                } catch (Exception e) {
                    if (e instanceof InterruptedException)
                        break;
                    e.printStackTrace();
                }
            }
        }
    };

//    /*
//     BLE Scan record parsing
//     inspired by:
//     http://stackoverflow.com/questions/22016224/ble-obtain-uuid-encoded-in-advertising-packet
//    */
//    static public Map<Integer, String> ParseRecord(byte[] scanRecord) {
//        Map<Integer, String> ret = new HashMap<Integer, String>();
//        int index = 0;
//        while (index < scanRecord.length) {
//            int length = scanRecord[index++];
//            //Zero value indicates that we are done with the record now
//            if (length == 0) break;
//
//            int type = scanRecord[index];
//            //if the type is zero, then we are pass the significant section of the data,
//            // and we are thud done
//            if (type == 0) break;
//
//            byte[] data = Arrays.copyOfRange(scanRecord, index + 1, index + length);
//            if (data.length > 0) {
//                StringBuilder hex = new StringBuilder(data.length * 2);
//                // the data appears to be there backwards
//                for (int bb = data.length - 1; bb >= 0; bb--) {
//                    hex.append(String.format("%02X", data[bb]));
//                }
//                ret.put(type, hex.toString());
//            }
//            index += length;
//        }
//
//        return ret;
//    }

    public Context getMyContext() {
        return myContext;
    }

    public CommandReceiver getCmdrec() {
        return cmdrec;
    }

    public boolean isNusfound() {
        return nusfound;
    }

    private void scanLeDevice(final boolean enable) {
        boolean result = false;
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothAdapter.stopLeScan(scanCallback);
                    Log.w(TAG, "Stopped LE Scan after " + SCAN_PERIOD / 1000 + " seconds.");
                }
            }, SCAN_PERIOD);

            mScanning = true;
            result = bluetoothAdapter.startLeScan(scanCallback);
            if (!result) {
                Log.w(TAG, "Start LE Scan failed: " + result);
            } else {
                Log.w(TAG, "Started LE Scan: " + result);
            }
        } else {
            mScanning = false;
            bluetoothAdapter.stopLeScan(scanCallback);
            Log.w(TAG, "Stopped LE Scan.");
        }
    }

    /**
     * Reads {@code headup_serial.txt} file, using the specified context to
     * get serial number of peripheral to connect to.
     */
    private String getDeviceSerialConfigured(Context myAppContext) {
        FileInputStream fis = null;
        try {

            File sdcard = Environment.getExternalStorageDirectory();
            File dir = new File(sdcard.getAbsolutePath() + "/headup/");
            dir.mkdir();
            File file = new File(dir, CONFIGFILE);
            fis = new FileInputStream(file);

            InputStreamReader inputStreamReader =
                    new InputStreamReader(fis, StandardCharsets.UTF_8);

            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                String line = reader.readLine();
                return line.substring(line.indexOf("=") + 1);
            } catch (IOException e) {
                // Error occurred when opening raw file for reading.
            }

        } catch (FileNotFoundException e) {
            try {

                File sdcard = Environment.getExternalStorageDirectory();
                File dir = new File(sdcard.getAbsolutePath() + "/headup/");
                dir.mkdir();
                File file = new File(dir, CONFIGFILE);
                FileOutputStream fos = new FileOutputStream(file);
                try {
                    // create file with default serial number
                    fos.write(new String("deviceserial=35895448783026136412").getBytes());
                    fos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            }

            e.printStackTrace();
        }
        return "";
    }

    public void destroy() {

        if (mBluetoothGatt != null) {
            BluetoothManager bluetoothManager = (BluetoothManager) myContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size() > 0)
                mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        ScanThread.interrupt();
        scanLeDevice(false);
        cmdrec.destroy();
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null && gatt.getDevice() != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception localException) {
            Log.e(TAG, "An exception occurred while refreshing device");
            localException.printStackTrace();
        }
        return false;
    }


}
