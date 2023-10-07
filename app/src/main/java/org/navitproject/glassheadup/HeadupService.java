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

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import java.util.Random;

/**
 * The main application service that manages the lifetime of the headup live card and the objects
 * that help out with orientation tracking and landmarks.
 */
public class HeadupService extends Service {

    private static final String LIVE_CARD_TAG = "headup";
    private static final long DELAY_MILLIS = 1000;
    private final HeadupBinder mBinder = new HeadupBinder();
    //private final Handler mHandler = new Handler();
    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable =
            new UpdateLiveCardRunnable();
    PowerManager.WakeLock wakeLock;
    private ConnectionManager mConnectionManager;
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;
    private BluetoothAdapter bluetoothAdapter = null;
    //private RemoteViews mLiveCardView;
    private View mLiveCardView;
    private int homeScore, awayScore;
    private Random mPointsGenerator;
    private NavImages navimages = new NavImages();
    private LiveCardRenderer mRenderer;
    private HeadupService me;
    PairingRequest pr;

    @Override
    public void onCreate() {
        super.onCreate();
        me = this;
        // TODO: Make the wifi status depending on a configuration file
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, LiveCardRenderer.class.getName());
        wakeLock.acquire(50000000);
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        bluetoothAdapter = bluetoothManager.getAdapter();
        Log.e(LIVE_CARD_TAG, "Scanmode: " + bluetoothAdapter.getScanMode());

        pr = new PairingRequest();

        bluetoothAdapter.disable();

        while (bluetoothAdapter.isEnabled()) ;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        bluetoothAdapter.enable();

        while (!bluetoothAdapter.isEnabled()) ;
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        bluetoothAdapter = null;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(pr, filter1);

        mConnectionManager = new ConnectionManager(this, bluetoothManager);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mLiveCard == null) {

            mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
            mRenderer = new LiveCardRenderer(mConnectionManager);
            mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mRenderer);

            // Display the options menu when the live card is tapped.
            Intent menuIntent = new Intent(this, HeadupMenuActivity.class);
            menuIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
            mLiveCard.attach(this);

            // Only reveal the card if the service was started explicitly by the user. If the
            // service dies in the background from some sort of error, we can recover when the
            // system restarts it automatically (because the compass is stateless), but we don't
            // want to disrupt the user by revealing the live card from out of nowhere. We detect
            // whether this was an automated restart by checking if the intent is null, and if so,
            // we publish silently instead.
            mLiveCard.publish((intent == null) ? PublishMode.SILENT : PublishMode.REVEAL);

        } else {
            mLiveCard.navigate();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        unregisterReceiver(mReceiver);
        unregisterReceiver(pr);

        mUpdateLiveCardRunnable.setStop(true);

        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

//        mSpeech.shutdown();
        mSpeech = null;
        mConnectionManager.destroy();
        mConnectionManager = null;
//        wakeLock.release();
        super.onDestroy();
    }

    /**
     * A binder that gives other components access to the speech capabilities provided by the
     * service.
     */
    public class HeadupBinder extends Binder {
        /**
         * Read the current heading aloud using the text-to-speech engine.
         */
        public void readHeadingAloud() {

        }
    }

    /**
     * Runnable that updates live card contents
     */
    private class UpdateLiveCardRunnable implements Runnable {

        private boolean mIsStopped = false;

        /*
         */
        public void run() {

        }

        public boolean isStopped() {
            return mIsStopped;
        }

        public void setStop(boolean isStopped) {
            this.mIsStopped = isStopped;
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);

                switch(state){
                    case BluetoothDevice.BOND_BONDING:
                        // Bonding...
                        Log.w("BOND", "Bonding");
                        break;

                    case BluetoothDevice.BOND_BONDED:
                        // Bonded...
                        Log.w("BOND", "Bonded");
                        me.unregisterReceiver(mReceiver);
                        break;

                    case BluetoothDevice.BOND_NONE:
                        // Not bonded...
                        Log.w("BOND", "FAILED BONDING");
                        break;
                }
            }
        }
    };

    public static class PairingRequest extends BroadcastReceiver {
        public PairingRequest() {
            super();
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin=intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, 0);
                    //the pin in case you need to accept for an specific pin
                    Log.e("PIN", " " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY",0));
                    //maybe you look for a name or address
                    //Log.d("Bonded", device.getName());
                    byte[] pinBytes;
                    pinBytes = (""+pin).getBytes("UTF-8");
                    device.setPin(pinBytes);
                    //setPairing confirmation if neeeded
                    device.setPairingConfirmation(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
