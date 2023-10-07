package org.navitproject.glassheadup;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.glass.timeline.DirectRenderingCallback;

import java.io.IOException;

import static org.navitproject.glassheadup.Utilities.readCoreValues;
import static org.navitproject.glassheadup.Utilities.readTemperature;

/**
 *
 */
public class LiveCardRenderer implements DirectRenderingCallback {


    private static final long FRAME_TIME_MILLIS = 1000;

    private SurfaceHolder mHolder;
    private boolean mPaused;
    private RenderThread mRenderThread;
    private ConnectionManager mConnectionManager;
    private RelativeLayout mLiveCardView;


    @SuppressLint("InflateParams")
    public LiveCardRenderer(ConnectionManager mConnectionManager) {
        this.mConnectionManager = mConnectionManager;

        // Get view from ressources
        mLiveCardView = (RelativeLayout) LayoutInflater.from(mConnectionManager.getMyContext()).inflate(R.layout.main_layout, null);
        mLiveCardView.setPadding(0, 0, 0, 0);

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
            if (mConnectionManager != null) {

                if (mConnectionManager.isNusfound()) {


                    if (mConnectionManager.getCmdrec().getNavstatus() >= 0) {

                        ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNextstreetname() != null ? mConnectionManager.getCmdrec().getNextstreetname() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getCurrentstreetname() != null ? mConnectionManager.getCmdrec().getCurrentstreetname() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText("ETA: " + String.valueOf(mConnectionManager.getCmdrec().getEtas() != null ? mConnectionManager.getCmdrec().getEtas() : ""));
                        ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNmls() != null ? mConnectionManager.getCmdrec().getNmls() : ""));

                        ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getCurspeed() != null ? String.format("%.1f", mConnectionManager.getCmdrec().getCurspeed()) : ""));

                        ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavimage()) != null ? NavImages.getNavImages().get(mConnectionManager.getCmdrec().getNavimage()) + "_wh" : "navit_96_96", "drawable", this.getClass().getPackage().getName())));

                        ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource((mLiveCardView.getResources().getIdentifier((mConnectionManager.getCmdrec().getNavstatus()) != 0 ? NavImages.getNavstatusimages().get(mConnectionManager.getCmdrec().getNavstatus()) + "_wh_96_96" : "status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName())));

                        ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdspeed()) + "km/h");

                        if (mConnectionManager.getCmdrec().getObdcoolanttemp() > 95)
                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FF0000"));
                        else if (mConnectionManager.getCmdrec().getObdcoolanttemp() < 80)
                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFF00"));
                        else
                            ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFFFF"));

                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdcoolanttemp()) + "°C");

                        ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getObdvoltage()) + "V");

                        ((TextView) (mLiveCardView.findViewById(R.id.distance_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getDlengths() != null ? mConnectionManager.getCmdrec().getDlengths() : ""));

                        String alt = ("\u2191" + (String.valueOf(mConnectionManager.getCmdrec().getGpsheights() != null ? mConnectionManager.getCmdrec().getGpsheights() : "")));
                        ((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view))).setText(alt);

                        ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlats() != null ? mConnectionManager.getCmdrec().getNavcoordlats() : ""));

                        ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getNavcoordlons() != null ? mConnectionManager.getCmdrec().getNavcoordlons() : ""));

                        ((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view))).setText(String.valueOf(mConnectionManager.getCmdrec().getDistances() != null ? mConnectionManager.getCmdrec().getDistances() : ""));


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


                } else {
                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource(R.drawable.navit_96_96);
                    ((TextView) (mLiveCardView.findViewById(R.id.nextstreetname_text_view))).setText("Navit Headup");
                    ((TextView) (mLiveCardView.findViewById(R.id.currentstreetname_text_view))).setText("Waiting for connection...");

                    ((TextView) (mLiveCardView.findViewById(R.id.eta_text_view))).setText("");
                    ((TextView) (mLiveCardView.findViewById(R.id.nextmanlength_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.currentspeed_text_view))).setText("");

                    ((ImageView) (mLiveCardView.findViewById(R.id.navimage_view))).setImageResource((mLiveCardView.getResources().getIdentifier("navit_96_96", "drawable", this.getClass().getPackage().getName())));

                    ((ImageView) (mLiveCardView.findViewById(R.id.navstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("status_no_destination_wh_96_96", "drawable", this.getClass().getPackage().getName()));

                    ((TextView) (mLiveCardView.findViewById(R.id.obdspeed_text_view))).setText("");

                    if (mConnectionManager.getCmdrec().getObdcoolanttemp() > 95)
                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FF0000"));
                    else if (mConnectionManager.getCmdrec().getObdcoolanttemp() < 80)
                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFF00"));
                    else
                        ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setTextColor(Color.parseColor("#FFFFFF"));

                    ((TextView) (mLiveCardView.findViewById(R.id.obdcoolanttemp_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.obdvoltage_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.distance_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.gpsheight_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.lat_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.lon_text_view))).setText("");

                    ((TextView) (mLiveCardView.findViewById(R.id.directdistance_text_view))).setText("");

                    ((ImageView) (mLiveCardView.findViewById(R.id.gpstatus_image_view))).setImageResource(mLiveCardView.getResources().getIdentifier("gui_strength_0_48_48", "drawable", this.getClass().getPackage().getName()));

                    ((ImageView) (mLiveCardView.findViewById(R.id.compas_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawCompassImage());

                    ((ImageView) (mLiveCardView.findViewById(R.id.routespeed_image_view))).setImageBitmap(mConnectionManager.getCmdrec().drawEmptyImage());
                }
            }
        }

        mLiveCardView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        mLiveCardView.layout(0, 0, mLiveCardView.getMeasuredWidth(), mLiveCardView.getMeasuredHeight());

        mLiveCardView.draw(canvas);

        float[] cpuload = readCoreValues();

        try {
            Log.w("TEMP", "CPU: " + readTemperature("/sys/devices/platform/notle_pcb_sensor.0/temperature") / 1000.0F);
            Log.w("TEMP", "Battery: " + readTemperature("/sys/devices/platform/omap_i2c.1/i2c-1/1-0055/power_supply/bq27520-0/temp") / 10.0F);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.w("LOAD", "CPUs:");
        int i = 0;
        for (float coreload : cpuload) {
            Log.w("LOAD", "Core" + i + ": " + String.format("%02.2f", coreload * 100) + "%");
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
}

