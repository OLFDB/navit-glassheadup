/*
 *
 *  * Navit, a modular navigation system.
 *  * Copyright (C) 2005-2008 Navit Team
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU General Public License
 *  * version 2 as published by the Free Software Foundation.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program; if not, write to the
 *  * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 *  * Boston, MA  02110-1301, USA.
 *
 */

package org.navitproject.glassheadup;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Environment;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

import static android.bluetooth.BluetoothAdapter.STATE_CONNECTED;
import static android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED;

/**
 * Handles the connection to the device running Navit
 */
public class ConnectionManager {

    private static final String TAG = ConnectionManager.class.getSimpleName();
    private final static int MIN_RSSI_VALUE = -70;
    private static final String CONFIGFILE = "headup_serial.txt"; // contains the serial number we connect to
    private static final long SCAN_PERIOD = 5000;
    private final static String NUS_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA8E";
    private final static String NUS_SERVICE_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA8E";
    private final static String NUS_SERVICE_UUID_SERIAL = "6E400004-B5A3-F393-E0A9-E50E24DCCA8E";
    private boolean connected = false;
    private BluetoothGatt mBluetoothGatt = null;
    private final byte[] cmdbuffer = new byte[50];
    private int cmdbufferidx = 0;
    private final CommandReceiver cmdrec;
    private final String deviceserial;
    private boolean nusfound = false;
    private boolean devicefound = false;
    private BluetoothGattCharacteristic nusrx = null;
    private BluetoothGattCharacteristic nussvcchar = null;

    private BluetoothGattService nusservice = null;

    private final Context myContext;
    private List<ScanResult> scanresults = null;
    private BluetoothLeScannerCompat scanner;
    private boolean listcomplete = false;
    private boolean restartscan;

    /**
     * Initializes a new instance of {@code ConnectionManager}, using the specified context to
     * access system services.
     */
    public ConnectionManager(Context applicationContext, BluetoothManager bluetoothManager) {
        myContext = applicationContext;
        cmdrec = new CommandReceiver(this);
        Log.d(TAG, "ConnectionManager() ");
        deviceserial = getDeviceSerialConfigured(myContext);
        Log.e(TAG, "CONFIGURED DEVICESERIAL IS: " + deviceserial);
        Log.e(TAG, "Start scanning.");
        ScanHandlerThread.start();
        scanLeDevice(true);
    }

    public boolean isConnected() {
        return connected;
    }

    private ScanCallback scanCallback = new ScanCallback() {

        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
        }

        public void onScanFailed(int errorCode) {
            Log.w(TAG, "Scan FAILED!");
        }

        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);

            Log.e(TAG, "Batch Scan - Count of results: " + results.size());
            if (results.size() > 0) {
                if (scanresults == null)
                    scanresults = new ArrayList<ScanResult>();
                scanner.stopScan(scanCallback);
                for (ScanResult res : results) {
                    if (devicefound)
                        break;
                    if (res.getRssi() < MIN_RSSI_VALUE)
                        continue;
                    scanresults.add(res);
                }
                listcomplete = true;
            } else {
                scanner.flushPendingScanResults(scanCallback);
            }
        }
    };

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.w(TAG, "onConnectionStateChange: " + getGATTStatus(status) + ", " + getGATTStatus(newState) + " gatt==mBluetoothGatt: " + (mBluetoothGatt == gatt));
            if (newState == STATE_CONNECTED) {

                Log.e(TAG, "A device connected");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                connected = gatt.discoverServices();

                if (!connected) {
                    gatt.disconnect();
                }

                Log.e(TAG, "DiscoverServices returned: " + connected);
            }

            if (newState == STATE_DISCONNECTED || newState == 133) { // https://issuetracker.google.com/issues/36976711
                Log.e(TAG, "STATE_DISCONNECTED: " + newState);
                if (mBluetoothGatt != null && nusrx != null)
                    mBluetoothGatt.setCharacteristicNotification(nusrx, false);
                nusrx = null;
                gatt.close();
                mBluetoothGatt = null;
                connected = false;
                nusfound = false;
                if (devicefound)
                    restartscan = true; // use variable to trigger new scan in ScanThread
                devicefound = false;
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
                return;
            }

            List<BluetoothGattService> svcs = gatt.getServices();

            if (svcs.size() < 2)
                Log.w(TAG, "No Services returned for Device: " + gatt.getDevice().getName());

            for (BluetoothGattService svc : svcs) {
                Log.w(TAG, "Service discovered: " + svc.getUuid().toString());
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
                        String deviceserial_read = characteristic.getStringValue(0);

                        if (nussvcchar != null) {
                            if (deviceserial_read.equals(deviceserial)) {
                                Log.e(TAG, "Found the configured device with serial " + deviceserial_read);
                                Log.e(TAG, "Gatt: " + mBluetoothGatt + " " + (gatt == mBluetoothGatt));

                                devicefound = true;
                            } else {
                                Log.e(TAG, "Wrong device, wrong serial number! " + deviceserial_read);
                                nusfound = false;
                                nusservice = null;
                                gatt.disconnect();
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
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }


                        nusrx = nusservice.getCharacteristic(UUID.fromString(NUS_SERVICE_UUID_RX));
                        if (gatt.setCharacteristicNotification(nusrx, true)) {
                            Log.w(TAG, "Subscribed to RX Notifictions successfuly.");
                        } else {
                            Log.e(TAG, "Subscription failed");
                        }

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        nusrx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }

                        BluetoothGattDescriptor descriptor = nusrx.getDescriptors().get(0);
                        boolean b = descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        Log.w(TAG, "ENABLE_NOTIFICATION_VALUE written: " + descriptor.getUuid() + " " + descriptor.getPermissions() + " " + b);
                        gatt.writeDescriptor(descriptor);
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
                    Log.e("DEBUG", "CMDBUFFERIDX out of range!");
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

    /**
     * Handle the scan results or start a new scan after connection has been lost.
     * restartscan is set to true in onConnectionStateChange
     * listcomplete is set true in onBatchScanResults if new result is available
     * devicefound is set in onCharacteristicRead
     */
    Thread ScanHandlerThread = new Thread() {
        public void run() {
            while (true) {
                if (restartscan) {
                    scanLeDevice(true);
                    restartscan = false;
                }
                if (listcomplete && scanresults != null) {
                    for (ScanResult res : scanresults) {
                        mBluetoothGatt = res.getDevice().connectGatt(myContext, false, mGattCallback);
                        if (mBluetoothGatt == null)
                            continue;

                        // In case the discovered services change cache needs refresh
                        refreshDeviceCache(mBluetoothGatt);

                        int i = 0;
                        // Wait for 10 seconds to handle connection and service discovery
                        while (!devicefound && i++ < 10) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {

                            }
                        }
                        if (devicefound)
                            break;
                        else if (mBluetoothGatt != null) {
                            try {
                                mBluetoothGatt.close();
                            } catch (Exception e) {

                            }
                        }
                    }
                    listcomplete = false;
                    // trigger new scan if we didn't find the device
                    if (!devicefound)
                        scanLeDevice(true);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
            }
        }
    };

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
        if (enable) {

            if (scanner == null)
                scanner = BluetoothLeScannerCompat.getScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setLegacy(false)
                    .setPhy(ScanSettings.PHY_LE_ALL_SUPPORTED)
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setMatchMode(ScanSettings.MATCH_MODE_STICKY)
                    .setReportDelay(3000) // use batch scan
                    .build();
            List<ScanFilter> filters = new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(null).build());
            try {
                scanner.startScan(filters, settings, scanCallback);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {

            if (scanner != null && scanCallback != null)
                scanner.stopScan(scanCallback);
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
                    fis = new FileInputStream(file);

                    InputStreamReader inputStreamReader =
                            new InputStreamReader(fis, StandardCharsets.UTF_8);

                    try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                        String line = reader.readLine();
                        return line.substring(line.indexOf("=") + 1);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
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
        ScanHandlerThread.interrupt();
        scanLeDevice(false);
        cmdrec.destroy();
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            if (gatt == null)
                return false;
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
