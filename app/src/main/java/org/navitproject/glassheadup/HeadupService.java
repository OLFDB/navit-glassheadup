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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Random;
import java.util.Set;

/**
 * The main application service that manages the lifetime of the headup live card
 */
public class HeadupService extends Service {

    private static final String LIVE_CARD_TAG = "headup";
    private static final long DELAY_MILLIS = 1000;
    private final HeadupBinder mBinder = new HeadupBinder();
    private final UpdateLiveCardRunnable mUpdateLiveCardRunnable =
            new UpdateLiveCardRunnable();
    PowerManager.WakeLock wakeLock;
    private ConnectionManager mConnectionManager;
    private TextToSpeech mSpeech;
    private LiveCard mLiveCard;
    private BluetoothAdapter bluetoothAdapter = null;
    private View mLiveCardView;
    private int homeScore, awayScore;
    private Random mPointsGenerator;
    private NavImages navimages = new NavImages();
    private LiveCardRenderer mRenderer;
    private HeadupService me;

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());

        // My glass displays time in GMT even I set the timezone via adb
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.setTimeZone("Europe/Berlin");

        File path = getFilesDir();

        try {
            InputStream stream = getAssets().open("timezone21.bin");
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            FileOutputStream os = new FileOutputStream(new File(path, "timezone21.bin"));
            os.write(buffer);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            InputStream stream = getAssets().open("country21.bin");
            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            FileOutputStream os = new FileOutputStream(new File(path, "country21.bin"));
            os.write(buffer);
            os.flush();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        mUpdateLiveCardRunnable.setStop(true);

        if (mLiveCard != null && mLiveCard.isPublished()) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }

//        mSpeech.shutdown();
        mSpeech = null;
        mConnectionManager.destroy();
        mConnectionManager = null;
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
}
