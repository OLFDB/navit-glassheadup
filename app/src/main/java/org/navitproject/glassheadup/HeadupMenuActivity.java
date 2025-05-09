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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.view.WindowUtils;

/**
 * This activity manages the options menu that appears when the user taps on the headup's live
 * card or says "ok glass" while the live card is settled.
 */
public class HeadupMenuActivity extends Activity {
    private boolean mAttachedToWindow;
    private HeadupService.HeadupBinder mHeadupService;
    private boolean mFromLiveCardVoice;
    private boolean mDoStop;
    private boolean mDoFinish;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof HeadupService.HeadupBinder) {
                mHeadupService = (HeadupService.HeadupBinder) service;
                openOptionsMenu();
                performActionsIfConnected();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Do nothing.
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        mFromLiveCardVoice = getIntent().getBooleanExtra(LiveCard.EXTRA_FROM_LIVECARD_VOICE, false);
        super.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mFromLiveCardVoice) {
            // When activated by voice from a live card, enable voice commands. The menu
            // will automatically "jump" ahead to the items (skipping the guard phrase
            // that was already said at the live card).
            getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        bindService(new Intent(this, HeadupService.class), mConnection, 0);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!mFromLiveCardVoice) {
            mAttachedToWindow = true;
            // When not activated by voice, we are activated by TAP from a live card.
            // Open the options menu as soon as window attaches.
            openOptionsMenu();
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAttachedToWindow = false;
    }

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (isMyMenu(featureId)) {
            getMenuInflater().inflate(R.menu.headup, menu);   // get content from
            return true;
        }
        return super.onCreatePanelMenu(featureId, menu);
    }

    @Override
    public boolean onPreparePanel(int featureId, View view, Menu menu) {
        if (isMyMenu(featureId)) {
            // Don't reopen menu once we are finishing. This is necessary
            // since voice menus reopen themselves while in focus.
            return !mDoFinish;
        }
        return super.onPreparePanel(featureId, view, menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (isMyMenu(featureId)) {
            switch (item.getItemId()) {
                case R.id.stop_this:
                    mDoStop = true;
                    return true;
            }
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onPanelClosed(int featureId, Menu menu) {
        super.onPanelClosed(featureId, menu);
        if (isMyMenu(featureId)) {
            // When the menu panel closes, either an item is selected from the menu or the
            // menu is dismissed by swiping down. Either way, we end the activity.
            mDoFinish = true;
            performActionsIfConnected();
        }
    }

    @Override
    public void openOptionsMenu() {
        if (mAttachedToWindow) {
            super.openOptionsMenu();
        }
    }

    /*
     * Performs the requested actions if connected. Since the connection may establish
     * either before or after actions are requested, we simply record requested actions,
     * and try to perform them both when the action establishes and when menu panel
     * closes (either due to dismissing or selecting an item).
     */
    private void performActionsIfConnected() {
        if (mHeadupService != null) {

            if (mDoStop) {
                mDoStop = false;
                stopService(new Intent(HeadupMenuActivity.this, HeadupService.class));
            }
            if (mDoFinish) {
                mHeadupService = null;
                unbindService(mConnection);
                finish();
            }
        }
    }

    /**
     * Returns {@code true} when the {@code featureId} belongs to the options menu or voice
     * menu that are controlled by this menu activity.
     */
    private boolean isMyMenu(int featureId) {
        return featureId == Window.FEATURE_OPTIONS_PANEL;// || featureId == WindowUtils.FEATURE_VOICE_COMMANDS;
    }
}
