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

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.AbsoluteSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.glass.timeline.DirectRenderingCallback;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import static org.navitproject.glassheadup.Utilities.readCoreValues;
import static org.navitproject.glassheadup.Utilities.readTemperature;

/**
 *
 */
public class LiveCardRenderer implements DirectRenderingCallback {


    private static final long FRAME_TIME_MILLIS = 1000;
    private static final int TIMEICONSIZE = 20;
    private static final int OBDICONSIZE = 20;

    private SurfaceHolder mHolder;
    private boolean mPaused;
    private RenderThread mRenderThread;
    private final ConnectionManager mConnectionManager;
    private final RelativeLayout mLiveCardView;
    private final Typeface firsttype;
    private final Typeface secondtype = Typeface.DEFAULT;


    @SuppressLint("InflateParams")
    public LiveCardRenderer(ConnectionManager mConnectionManager) {
        this.mConnectionManager = mConnectionManager;

        // Get view from ressources
        mLiveCardView = (RelativeLayout) LayoutInflater.from(mConnectionManager.getMyContext()).inflate(R.layout.main_layout, null);
        mLiveCardView.setPadding(0, 0, 0, 0);
        firsttype = Typeface.createFromAsset(mConnectionManager.getMyContext().getAssets(),
                "fonts/materialsymbols.ttf");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format,
                               int width, int height) {
        // Update your views accordingly.
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mPaused = false;
        mHolder = holder;
        updateRendering();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mHolder = null;
        updateRendering();
    }

    @Override
    public void renderingPaused(SurfaceHolder holder, boolean paused) {
        mPaused = paused;
        updateRendering();
    }

    /**
     * Start or stop rendering according to the timeline state.
     */
    private void updateRendering() {
        boolean shouldRender = (mHolder != null) && !mPaused;
        boolean rendering = mRenderThread != null;

        if (shouldRender != rendering) {
            if (shouldRender) {
                mRenderThread = new RenderThread();
                mRenderThread.start();
            } else {
                mRenderThread.quit();
                mRenderThread = null;
            }
        }
    }

    /**
     * Draws the view in the SurfaceHolder's canvas.
     */
    private void draw() {
        Canvas canvas;
        try {
            canvas = mHolder.lockCanvas();
        } catch (Exception e) {
            return;
        }
        if (canvas != null) {
            // Draw on the canvas.
            // Clear Canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY);

            // Update the remote view with the new scores.
//            if (mConnectionManager != null) {
//
//                if (mConnectionManager.isNusfound()) {
//
//
//                    if (mConnectionManager.getCmdrec().getNavstatus() >= 0) {
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNextstreetname() != null ? mConnectionManager.getCmdrec().getNextstreetname() : ""));
//                        ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getCurrentstreetname() != null ? mConnectionManager.getCmdrec().getCurrentstreetname() : ""));
//                        ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText("ETA: " + String.valueOf(mConnectionManager.getCmdrec().getEtas() != null ? mConnectionManager.getCmdrec().getEtas() : ""));
//                        ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNmls() != null ? mConnectionManager.getCmdrec().getNmls() : ""));
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getCurspeed() != null ? String.format("%.1f", mConnectionManager.getCmdrec().getCurspeed()) : ""));
//
//                        ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavimage()) != null ? NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) + "_wh" : "navit_96_96", "drawable", this.getClass().getPackage().getName())));
//
//                        ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavstatus()) != 0 ? NavImages.getNavstatusimages().get(mConnectionManager.getCmdrec().getNavstatus()) + "_wh_96_96" : "status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName())));
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdspeed()) + "km/h");
//
//                        if (mConnectionManager.getCmdrec().getObdcoolanttemp() > 95)
//                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FF0000"));
//                        else if (mConnectionManager.getCmdrec().getObdcoolanttemp() < 80)
//                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFF00"));
//                        else
//                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFFFF"));
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdcoolanttemp()) + "°C");
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdvoltage()) + "V");
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.distance_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getDestinationLengths() != null ? mConnectionManager.getCmdrec().getDestinationLengths() : ""));
//
//                        String alt = ("\u2191" + (String.valueOf(mConnectionManager.getCmdrec().getGpsheights() != null ? mConnectionManager.getCmdrec().getGpsheights() : "")));
//                        ((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view))).setText(alt);
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlats() != null ? mConnectionManager.getCmdrec().getNavcoordlats() : ""));
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlons() != null ? mConnectionManager.getCmdrec().getNavcoordlons() : ""));
//
//                        ((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getDistances() != null ? mConnectionManager.getCmdrec().getDistances() : ""));
//
//
//                        ((ImageView) (mLiveCardView.findViewById(R.id.gpstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier(NavImages.getGpssignalstrengthimages().get(mConnectionManager.getCmdrec().getSatcnt()) != null ? NavImages.getGpssignalstrengthimages().get(mConnectionManager.getCmdrec().getSatcnt()) + "_48_48" : "gui_strength_0_48_48", "drawable", this.getClass().getPackage().getName()));
//
//                        ((ImageView) (mLiveCardView.findViewById(R.id.compas_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawCompassImage());
//
//                        if (mConnectionManager.getCmdrec().getCurroutespeed() > 0.0) {
//                            ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawSpeedLimitImage());
//                        } else {
//
//                            ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawEmptyImage());
//                        }
//                    } else {
//                        ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource(mLiveCardView.getResources().getIdentifier(NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) != null ? NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) + "_wh" : "navit_96_96", "drawable", this.getClass().getPackage().getName()));
//                    }
//
//
//                } else {
//                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource(R.drawable.navit_96_96);
//                    ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText("Navit Headup");
//                    ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText("Waiting for connection...");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText("");
//                    ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText("");
//
//                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier("navit_96_96", "drawable", this.getClass().getPackage().getName())));
//
//                    ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName()));
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText("");
//
//                    if (mConnectionManager.getCmdrec().getObdcoolanttemp() > 95)
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FF0000"));
//                    else if (mConnectionManager.getCmdrec().getObdcoolanttemp() < 80)
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFF00"));
//                    else
//                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFFFF"));
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.distance_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText("");
//
//                    ((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view))).setText("");
//
//                    ((ImageView) (mLiveCardView.findViewById(R.id.gpstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("gui_strength_0_48_48", "drawable", this.getClass().getPackage().getName()));
//
//                    ((ImageView) (mLiveCardView.findViewById(R.id.compas_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawCompassImage());
//
//                    ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawEmptyImage());
//                }
//            }

//            // Update the remote view with the new data.
            if (mConnectionManager != null && mRenderThread.shouldRun()) {

                String timezone = ConversionHelper.getTimezone(Float.parseFloat(mConnectionManager.getCmdrec().getNavcoordlat().toString()), Float.parseFloat(mConnectionManager.getCmdrec().getNavcoordlon().toString()));
                Date date = new Date();
                @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("HH:mm");

                if (mConnectionManager.isNusfound()) {


                    if (mConnectionManager.getCmdrec().getNavstatus() >= 0) {

                        ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNextstreetname() != null ? mConnectionManager.getCmdrec().getNextstreetname() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getCurrentstreetname() != null ? mConnectionManager.getCmdrec().getCurrentstreetname() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText(String.format(Locale.UK, "%s%s", mLiveCardView.getResources().getString(R.string.etatxt), mConnectionManager.getCmdrec().getEtas() != null ? mConnectionManager.getCmdrec().getEtas() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNmls() != null ? mConnectionManager.getCmdrec().getNmls() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText(mConnectionManager.getCmdrec().getCurspeed() != null ? String.format(Locale.UK, "%.1f", mConnectionManager.getCmdrec().getCurspeed()) : "");
                        ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavimage()) != null ? NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) + "_wh" : "navit_96_96", "drawable", this.getClass().getPackage().getName())));
                        ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavstatus()) != 0 ? NavImages.getNavstatusimages().get(mConnectionManager.getCmdrec().getNavstatus()) + "_wh_96_96" : "status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName())));

                        setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view)), "\ue9e4", mConnectionManager.getCmdrec().getObdspeed() + "km/h", OBDICONSIZE);

                        if (mConnectionManager.getCmdrec().getObdcoolanttemp() != -255) {

                            if (mConnectionManager.getCmdrec().getObdcoolanttemp() > 105)
                                ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FF0000"));
                            else if (mConnectionManager.getCmdrec().getObdcoolanttemp() < 80 || (mConnectionManager.getCmdrec().getObdcoolanttemp() > 95 && mConnectionManager.getCmdrec().getObdcoolanttemp() <= 105))
                                ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFF00"));
                            else
                                ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFFFF"));

                            setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view)), "\ue846", mConnectionManager.getCmdrec().getObdcoolanttemp() + "°C", OBDICONSIZE);
                            setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view)), "\uea0b", mConnectionManager.getCmdrec().getObdvoltage() + "V", OBDICONSIZE);

                        } else {
                            ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText("");
                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText("");
                            ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText("");
                        }

                        setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.distance_text_view)), "\ueacd", String.valueOf(mConnectionManager.getCmdrec().getDestinationLengths() != null ? mConnectionManager.getCmdrec().getDestinationLengths() : ""), TIMEICONSIZE);

                        String alt = ((String.valueOf(mConnectionManager.getCmdrec().getGpsheights() != null ? mConnectionManager.getCmdrec().getGpsheights() : "")));
                        setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view)), "\uf873", alt, OBDICONSIZE);

                        ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlats() != null ? mConnectionManager.getCmdrec().getNavcoordlats() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlons() != null ? mConnectionManager.getCmdrec().getNavcoordlons() : ""));

                        setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view)), "\uf6ea", String.valueOf(mConnectionManager.getCmdrec().getDistances() != null ? mConnectionManager.getCmdrec().getDistances() : ""), OBDICONSIZE);

                        ((ImageView) (mLiveCardView.findViewById(R.id.gpstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier(NavImages.getGpssignalstrengthimages().get(mConnectionManager.getCmdrec().getSatcnt()) != null ? NavImages.getGpssignalstrengthimages().get(mConnectionManager.getCmdrec().getSatcnt()) + "_48_48" : "gui_strength_0_48_48", "drawable", this.getClass().getPackage().getName()));
                        ((ImageView) (mLiveCardView.findViewById(R.id.compas_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawCompassImage());

                        if (mConnectionManager.getCmdrec().getCurroutespeed() > 0.0) {
                            ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawSpeedLimitImage());
                        } else {
                            ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawEmptyImage());
                        }
                    } else {
                        ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource(mLiveCardView.getResources().getIdentifier(NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) != null ? NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) + "_wh" : "navit_96_96", "drawable", this.getClass().getPackage().getName()));
                    }

                    if(timezone != null) {
                        TimeZone tz = TimeZone.getTimeZone(timezone);
                        if(tz != null) {
                            df.setTimeZone(tz);
                        }
                    }
                } else {
                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource(R.drawable.navit_96_96);
                    ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText(R.string.greeter);
                    ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText(R.string.waitconnection);
                    ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText("");
                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier("navit_96_96", "drawable", Objects.requireNonNull(this.getClass().getPackage()).getName())));
                    ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName()));
                    ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.distance_text_view))).setText("");

                    setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view)), "\uf873", "", 18);

                    ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view))).setText("");
                    ((ImageView) (mLiveCardView.findViewById(R.id.gpstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("gui_strength_0_48_48", "drawable", this.getClass().getPackage().getName()));
                    ((ImageView) (mLiveCardView.findViewById(R.id.compas_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawCompassImage());
                    ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawEmptyImage());
                }

                setTextWithIcon((TextView) (mLiveCardView.findViewById(R.id.time_view)), "\ue8b5", df.format(date), TIMEICONSIZE);

            }


        }

        mLiveCardView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mLiveCardView.layout(0, 0, mLiveCardView.getMeasuredWidth(), mLiveCardView.getMeasuredHeight());

        mLiveCardView.draw(canvas);

        float[] cpuload = readCoreValues();

        try {
            Log.i("TEMP", "CPU: " + readTemperature("/sys/devices/platform/notle_pcb_sensor.0/temperature") / 1000.0F);
            Log.i("TEMP", "Battery: " + readTemperature("/sys/devices/platform/omap_i2c.1/i2c-1/1-0055/power_supply/bq27520-0/temp") / 10.0F);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("LOAD", "CPUs:");
        int i = 0;
        for (float coreload : cpuload) {
            Log.i("LOAD", "Core" + i + ": " + String.format("%02.2f", coreload * 100) + "%");
        }

        mHolder.unlockCanvasAndPost(canvas);
    }


    /**
     * Redraws in the background.
     */
    private class RenderThread extends Thread {
        private boolean mShouldRun;

        /**
         * Initializes the background rendering thread.
         */
        public RenderThread() {
            mShouldRun = true;
        }

        /**
         * Returns true if the rendering thread should continue to run.
         *
         * @return true if the rendering thread should continue to run
         */
        private synchronized boolean shouldRun() {
            return mShouldRun;
        }

        /**
         * Requests that the rendering thread exit at the next
         * opportunity.
         */
        public synchronized void quit() {
            mShouldRun = false;
        }

        @Override
        public void run() {
            while (shouldRun()) {
                draw();
                SystemClock.sleep(FRAME_TIME_MILLIS);
            }
        }
    }

    public void setTextWithIcon(TextView textView, String iconcode, String text, int iconsize) {

        if (text.equals("")) {
            textView.setText("");
            return;
        }

        Spannable spannable = new SpannableString(iconcode + text);
        spannable.setSpan(new AbsoluteSizeSpan(iconsize), 0, iconcode.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new CustomTypefaceSpan("materialfont", firsttype), 0, iconcode.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new CustomTypefaceSpan("materialfont", secondtype), iconcode.length(), iconcode.length() + text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        textView.setText(spannable);
    }
}

