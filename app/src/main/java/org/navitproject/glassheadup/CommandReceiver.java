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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

import static android.content.ContentValues.TAG;

public class CommandReceiver {
    private static final int BTHEADUP_DISTANCE = 0x10;
    private static final int BTHEADUP_DIRECTION = 0x11;
    private static final int BTHEADUP_IMPERIAL = 0x12;
    private static final int BTHEADUP_NAVSTATUS = 0x13;
    private static final int BTHEADUP_COORDINATESGEO = 0x14;
    private static final int BTHEADUP_NAVNEXTTURNIMAGE = 0x15;
    //    private static final int BTHEADUP_GPSNUMSATSUSED = 0x16;
    private static final int BTHEADUP_GPSHDOP = 0x17;
    private static final int BTHEADUP_STREETNAME = 0x18;
    //    private static final int BTHEADUP_STREETSYSNAME = 0x19;
    private static final int BTHEADUP_DESTTIME = 0x1A;
    private static final int BTHEADUP_DESTLENGTH = 0x1B;
    private static final int BTHEADUP_NEXTSTREETNAME = 0x1C;
    //    private static final int BTHEADUP_NEXTSTREETSYSNAME = 0x1D;
    private static final int BTHEADUP_NEXTMANEUVLENGTH = 0x1E;
    private static final int BTHEADUP_GPSSIGNALSTRENGTH = 0x1F;
    private static final int BTHEADUP_ROUTESPEED = 0x20;
    private static final int BTHEADUP_HANDLEDIRECTION = 0x21;
    private static final int BTHEADUP_VEHICLESPEED = 0x22;
    private static final int BTHEADUP_GPSHEIGHT = 0x23;
    //    private static final int BTHEADUP_CONTRAST = 0x24;
    //    private static final int BTHEADUP_ORIENTATION = 0x25;
    private static final int BTHEADUP_OBDSPEED = 0x26;
    private static final int BTHEADUP_OBDVOLTAGE = 0x27;
    private static final int BTHEADUP_OBDOILTEMP = 0x28;
    private static final int BTHEADUP_OBDCOOLANTTEMP = 0x29;

    private static final int CMDBUFFERSIZE = 20;
    private static final double SPEEDLIMITEXCEEDRED = 10;
    private Double dir = 0.0;
    private Double hdir = 0.0;
    private String distances = "";
    private Double curroutespeed = 0.0;
    private Double curspeed = 0.0;
    private Double navcoordlat = 0.0;
    private String navcoordlats = "";
    private Double navcoordlon = 0.0;
    private String navcoordlons = "";
    private String etas = "";
    private String dlengths = "";
    private String nmls = "";
    private Integer satcnt = 0;
    private String gpsheights = "";
    private String nextstreetname = "";
    private String currentstreetname = "";
    private Double gpsheight = 0.0;
    private int navstatus = 0;
    private double obdspeed = 0.0;
    private boolean processing = false;
    private double obdoiltemp = 0.0;
    private double obdcoolanttemp = 0.0;
    private double obdvoltage = 0.0;
    private Integer navimage;
    private int imperial = 0;
    private ConnectionManager conmgr = null;

    private ArrayList<commands> cmds = new ArrayList<>(CMDBUFFERSIZE);

    Thread CmDThread = new Thread() {
        public void run() {
            while (true) {
                try {
                    if(conmgr.isNusfound()) {
                        processcmd();
                        Log.d(TAG, "processCmd");
                    }
                    Thread.sleep(900);
                } catch (Exception e) {
                    if (e instanceof InterruptedException)
                        break;
                    e.printStackTrace();
                }
            }
        }
    };

    public CommandReceiver(ConnectionManager connectionManager) {
        conmgr = connectionManager;
        int i = 0;
        while (i < CMDBUFFERSIZE) {
            cmds.add(new commands());
            i++;
        }
        CmDThread.start();
    }

    public static double doubleFromByteArray(byte[] bytes, int position) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, position, 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    // Compass
    public static void draw_compass(Canvas canvas, Point p, int r, int dir, Paint paint) {
        Point[] ph = new Point[3];
        for (int index = 0; index < ph.length; index++) {
            ph[index] = new Point();
        }

        int l = (int) (r * 0.25);

        ph[0].x = -l;
        ph[0].y = 0;
        ph[1].x = 0;
        ph[1].y = -r;
        ph[2].x = l;
        ph[2].y = 0;
        transform_rotate(p, dir, ph, 3); /* Rotate to the correct direction */
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(Color.RED);
        Path path = new Path();
        path.moveTo(ph[0].x, ph[0].y);
        path.lineTo(ph[1].x, ph[1].y);
        path.lineTo(ph[2].x, ph[2].y);
        path.close();
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.STROKE);
        ph[0].x = -l;
        ph[0].y = 0;
        ph[1].x = 0;
        ph[1].y = r;
        ph[2].x = l;
        ph[2].y = 0;
        transform_rotate(p, dir, ph, 3); /* Rotate to the correct direction */
        paint.setColor(Color.WHITE);
        canvas.drawLine(ph[0].x, ph[0].y, ph[1].x, ph[1].y, paint);
        canvas.drawLine(ph[1].x, ph[1].y, ph[2].x, ph[2].y, paint);
        canvas.drawLine(ph[0].x, ph[0].y, ph[2].x, ph[2].y, paint);

    }

    // Arrow
    public static void draw_handle(Canvas canvas, Point p, int r, int dir, Paint paint) {
        paint.setColor(Color.GREEN);
        Point[] ph = new Point[3];
        for (int index = 0; index < ph.length; index++) {
            ph[index] = new Point();
        }
        double l = r * 0.4;

        ph[0].x = 0; /* Compute details for the body of the arrow */
        ph[0].y = r - 8;
        ph[1].x = 0;
        ph[1].y = -r + 8;
        transform_rotate(p, dir, ph, 2); /* Rotate to the correct direction */
        canvas.drawLine(ph[0].x, ph[0].y, ph[1].x, ph[1].y, paint); /* Draw the body */

        ph[0].x = (int) (-l / 2); /* Compute details for the head of the arrow */
        ph[0].y = (int) (-r + 4 + l / 2);
        ph[1].x = 0;
        ph[1].y = -r + 4;
        ph[2].x = (int) (l / 2);
        ph[2].y = (int) (-r + 4 + l / 2);
        transform_rotate(p, dir, ph, 3); /* Rotate to the correct direction */
        /* Draw the head */
        Path path = new Path();
        path.moveTo(ph[0].x, ph[0].y);
        path.lineTo(ph[1].x, ph[1].y);
        path.lineTo(ph[2].x, ph[2].y);
        canvas.drawPath(path, paint);
    }

    public static void transform_rotate(Point center, int angle, Point[] p, int count) {
        int i, x, y;
        double dx, dy;
        for (i = 0; i < count; i++) {
            dx = Math.sin(Math.PI * angle / 180.0);
            dy = Math.cos(Math.PI * angle / 180.0);
            x = (int) (dy * p[i].x - dx * p[i].y);
            y = (int) (dx * p[i].x + dy * p[i].y);

            p[i].x = center.x + x;
            p[i].y = center.y + y;
        }
    }

    public int getImperial() {
        return imperial;
    }

    public double getObdspeed() {
        return obdspeed;
    }

    public double getObdoiltemp() { return obdoiltemp; }

    public double getObdcoolanttemp() {
        return obdcoolanttemp;
    }

    public double getObdvoltage() {
        return obdvoltage;
    }

    public Integer getNavimage() {
        return navimage;
    }

    public Double getDir() {
        return dir;
    }

    public Double getHdir() {
        return hdir;
    }

    public String getDistances() {
        return distances;
    }

    public Double getCurroutespeed() {
        return curroutespeed;
    }

    public Double getCurspeed() {
        return curspeed;

    }

    public Double getNavcoordlat() {
        return navcoordlat;
    }

    public String getNavcoordlats() {
        return navcoordlats;
    }

    public Double getNavcoordlon() {
        return navcoordlon;
    }

    public String getNavcoordlons() {
        return navcoordlons;
    }

    public String getEtas() {
        return etas;
    }

    public String getDestinationLengths() {
        return dlengths;
    }

    public String getNmls() {
        return nmls;
    }

    public Integer getSatcnt() {
        return satcnt;
    }

    public String getGpsheights() {
        return gpsheights;
    }

    public String getNextstreetname() {
        return nextstreetname;
    }

    public String getCurrentstreetname() {
        return currentstreetname;
    }

    public Double getGpsheight() {
        return gpsheight;
    }

    public void assembleCommand(byte[] data, int len) {

        boolean receive_data = true;
        int index = 0xFFFF;

        for (int i = 0; i < CMDBUFFERSIZE; i++) {
            if (!cmds.get(i).used || cmds.get(i).data[0] == data[0]) { // use buffer for same command again
                index = i;
                cmds.get(i).used = true;
                break;
            }
        }

        if (index == 0xFFFF) {
            Log.d(TAG, "No buffer for RX data!");
            return;
        }

        cmds.get(index).length = len;

        // copy to buffer and set flag for main loop
        if (len >= 0) System.arraycopy(data, 0, cmds.get(index).data, 0, len);
    }

    private int crc16_compute(byte[] bytes, int length) {

        byte[] data = new byte[length];

        System.arraycopy(bytes, 0, data, 0, length);

        int crc = 0xFFFF;
        int polynomial = 0x1021;

        for (byte b : data) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;

        return (int) crc;
    }

    int intFromByteArray(byte[] bytes) {
        return ((bytes[2 + 3] & 0xFF) << 24) |
                ((bytes[2 + 2] & 0xFF) << 16) |
                ((bytes[2 + 1] & 0xFF) << 8) |
                ((bytes[2] & 0xFF));
    }

    public int getNavstatus() {
        return (navstatus == 3) ? 4 : navstatus;
    }

    private void processcmd() {

        int cmdidx;
        double dbuffer = 0.0;
        double cnv = 0.0;

        if (processing)
            return;

        processing = true;

        for (cmdidx = 0; cmdidx < CMDBUFFERSIZE; cmdidx++) {
            if (cmds.get(cmdidx).used && cmds.get(cmdidx).length > 4) {

                Log.d(TAG, "CMDBUFFERIDX: " + cmdidx);

                if (cmds.get(cmdidx).data[1] > cmds.get(cmdidx).length - 2) {
                    cmds.get(cmdidx).used = false;
                    Log.e(TAG, "Received data length > real data length!");
                    break;
                }
                // check CRC
                int crc = crc16_compute(cmds.get(cmdidx).data, cmds.get(cmdidx).length - 4);
                int rcrc = ((cmds.get(cmdidx).data[cmds.get(cmdidx).data[1] - 2] & 0xFF) << 8) + (cmds.get(cmdidx).data[cmds.get(cmdidx).data[1] - 1] & 0xFF);

                if (crc == rcrc) {
                    switch (cmds.get(cmdidx).data[0]) {
                        case BTHEADUP_DIRECTION:
                            dir = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_DIRECTION: " + dir);
                            break;

                        case BTHEADUP_HANDLEDIRECTION:
                            hdir = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_HANDLEDIRECTION: " + hdir);
                            break;

                        case BTHEADUP_DISTANCE:
                            distances = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_DISTANCE: " + distances);
                            break;

                        case BTHEADUP_COORDINATESGEO:
                            navcoordlat = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            navcoordlats = navcoordlat >= 0 ? String.format(Locale.getDefault(), "N%.5f", navcoordlat < 0 ? navcoordlat * -1 : navcoordlat) : String.format(Locale.getDefault(), "S%.5f", navcoordlat < 0 ? navcoordlat * -1 : navcoordlat);
                            navcoordlon = doubleFromByteArray(cmds.get(cmdidx).data, 10);
                            navcoordlons = navcoordlon >= 0 ? String.format(Locale.getDefault(), "E%.5f", navcoordlon < 0 ? navcoordlon * -1 : navcoordlon) : String.format(Locale.getDefault(), "W%.5f", navcoordlon < 0 ? navcoordlon * -1 : navcoordlon);
                            Log.i(TAG, "BTHEADUP_COORDINATESGEO: " + String.format("%s %s", navcoordlats, navcoordlons));
                            break;

                        case BTHEADUP_DESTTIME:
                            etas = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_DESTTIME: " + String.format("%s", etas));
                            break;

                        case BTHEADUP_DESTLENGTH:

                            dlengths = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_DESTLENGTH: " + String.format("%s", dlengths));
                            break;

                        case BTHEADUP_NEXTMANEUVLENGTH:
                            nmls = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_NEXTMANEUVLENGTH: " + String.format("%s", nmls));
                            break;

                        case BTHEADUP_ROUTESPEED:
                            curroutespeed = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_ROUTESPEED:" + String.format("%.0f", curroutespeed));
                            break;

                        case BTHEADUP_VEHICLESPEED:
                            curspeed = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_VEHICLESPEED: " + String.format("%.0f", curspeed));
                            break;

                        case BTHEADUP_GPSSIGNALSTRENGTH:
                            satcnt = intFromByteArray(cmds.get(cmdidx).data);
                            Log.i(TAG, "BTHEADUP_GPSSIGNALSTRENGTH: " + String.format("%d", satcnt));
                            break;

                        case BTHEADUP_GPSHEIGHT:
                            gpsheight = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            gpsheights = String.format(Locale.getDefault(), "%.0fm", gpsheight); // TODO: change to String on sender side (imperial)
                            Log.i(TAG, "BTHEADUP_GPSHEIGHT: " + gpsheights);
                            break;

                        case BTHEADUP_STREETNAME:
                            currentstreetname = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_STREETNAME: " + currentstreetname);
                            break;

                        case BTHEADUP_NEXTSTREETNAME:
                            nextstreetname = new String(cmds.get(cmdidx).data, 2, cmds.get(cmdidx).length - 7, StandardCharsets.UTF_8);
                            Log.i(TAG, "BTHEADUP_NEXTSTREETNAME: " + String.format("%s", nextstreetname));
                            break;

                        case BTHEADUP_NAVNEXTTURNIMAGE:
                            navimage = intFromByteArray(cmds.get(cmdidx).data);
                            Log.i(TAG, "BTHEADUP_NAVNEXTTURNIMAGE: " + navimage);
                            break;

                        case BTHEADUP_NAVSTATUS:
                            navstatus = intFromByteArray(cmds.get(cmdidx).data);
                            Log.i(TAG, "BTHEADUP_NAVSTATUS: " + navstatus);
                            break;

                        case BTHEADUP_GPSHDOP:
                            // ignore
                            break;

                        case BTHEADUP_OBDSPEED:
                            obdspeed = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_OBDSPEED: " + String.format("%.0fkm/h", obdspeed));
                            break;

                        case BTHEADUP_OBDCOOLANTTEMP:
                            obdcoolanttemp = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_OBDCOOLANTTEMP: " + String.format("%.0f°C", obdcoolanttemp));
                            break;

                        case BTHEADUP_OBDOILTEMP:
                            obdoiltemp = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_OBDOILTEMP: " + String.format("%.0f°C", obdoiltemp));
                            break;

                        case BTHEADUP_OBDVOLTAGE:
                            obdvoltage = doubleFromByteArray(cmds.get(cmdidx).data, 2);
                            Log.i(TAG, "BTHEADUP_OBDVOLTAGE: " + String.format("%.1fV", obdvoltage));
                            break;

                        case BTHEADUP_IMPERIAL:
                            imperial = intFromByteArray(cmds.get(cmdidx).data);
                            Log.i(TAG, "BTHEADUP_IMPERIAL: " + imperial);
                            break;

                        default:
                            Log.w(TAG, "Unknown Command: " + cmds.get(cmdidx).data[0])
                            ;
                            break;
                    }

                } else {
                    Log.e(TAG, "CRC failure");
                }

                cmds.get(cmdidx).used = false;

            } else {
                cmds.get(cmdidx).used = false; // in case length <=4
            }
        }

        processing = false;
    }

    public Bitmap drawCompassImage() {
        int w = 200, h = 200;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
        Canvas canvas = new Canvas(bmp);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        canvas.drawCircle(100, 100, 95, paint);

        Point center = new Point(100, 100);

        draw_handle(canvas, center, 90, hdir.intValue(), paint);
        draw_compass(canvas, center, 90, dir.intValue(), paint);

        return bmp;

    }

    public Bitmap drawEmptyImage() {
        int w = 200, h = 200;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.BLACK);

        return bmp;
    }

    public Bitmap drawSpeedLimitImage() {
        int w = 200, h = 200;

        Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
        Bitmap bmp = Bitmap.createBitmap(w, h, conf); // this creates a MUTABLE bitmap
        Canvas canvas = new Canvas(bmp);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20);
        canvas.drawCircle(100, 100, 80, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawCircle(100, 100, 55, paint);

        if(curspeed - curroutespeed > SPEEDLIMITEXCEEDRED) {
            paint.setColor(Color.RED);
        } else {
            paint.setColor(Color.BLACK);
        }

        paint.setStrokeWidth(1);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format(Locale.getDefault(), "%.0f", curroutespeed), 98, 120, paint);

        return bmp;

    }

    public void destroy() {
        CmDThread.interrupt();
    }

    private static class commands {
        byte[] data = new byte[50];
        boolean used;
        int length = 0;

        public commands() {
            used = false;
        }
    }
}
