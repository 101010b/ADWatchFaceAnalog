/*
 * Copyright (C) 2015 Thomas Buck
 * Based on com.example.android.wearable.watchface from the Android SDK
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

package com.alphadraco.watchface.analog;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Simple analog watch face
 */
public class AnalogWatchFaceService extends CanvasWatchFaceService {
    private static final String TAG = "AnalogWatchFaceService";

    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);


    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    //implements MessageApi.MessageListener,
    //GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
    private class Engine extends CanvasWatchFaceService.Engine
        // implements SensorEventListener
    {
        static final int MSG_UPDATE_TIME = 0;


        // Hands
        Paint   mMinutePaint, mMinutePaint_ambient,
                mMinuteIndicatorPaint, mMinuteIndicatorPaint_ambient;
        Paint   mHourPaint, mHourPaint_ambient,
                mHourIndicatorPaint, mHourIndicatorPaint_ambient;
        Paint   mSecondPaint; // No ambient - does not exist in ambient mode

        // Auxiliary Hands - for Timer and Countdown
        Paint   mTimerHandPaint, mTimerHandPaint_ambient;
        Paint   mCountdownHandPaint, mCountdownHandPaint_ambient;

        // Ticks
        Paint   mZeroTickPaint, mZeroTickPaint_ambient,
                mHourTickPaint, mHourTickPaint_ambient,
                mMinuteTickPaint, mMinuteTickPaint_ambient;

        // Center
        Paint mCenterPaint, mCenterPaint_ambient;

        // Text
        Paint   mBoldTextPaint, mBoldTextPaint_ambient,
                mNormalTextPaint, mNormalTextPaint_ambient,
                mNormalRedTextPaint, mNormalRedTextPaint_ambient;


        boolean mMute;
        Time mTime;
        long mTimer = 0;
        long mCountdown = 0;

        boolean mTimerHand=false;
        boolean mCountdownHand=false;

        boolean cntDiffdefined=false;
        long cntDiffold=0;

        /*
        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something here if sensor accuracy changes.
        }

        @Override
        public final void onSensorChanged(SensorEvent event) {
            // float millibars_of_pressure = event.values[0];
            // Do something with this sensor data.
            temperature = event.values[0];

        }
        */

        /** Handler to update the time once a second in interactive mode. */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        if (Log.isLoggable(TAG, Log.VERBOSE)) {
                            Log.v(TAG, "updating time");
                        }
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;


        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        //Bitmap mBackgroundBitmap;
        //Bitmap mBackgroundScaledBitmap;

        // Battery Status
        boolean batterycharging=false;
        float batteryfill=0.0f;
        //float temperature=0.0f;

        @Override
        public void onCreate(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onCreate");
            }
            super.onCreate(holder);

            AnalogWatchFaceService.this.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,-1);
                    if ((status == BatteryManager.BATTERY_STATUS_CHARGING) ||
                            (status == BatteryManager.BATTERY_STATUS_FULL)) {
                        // Charge mode
                        if (batterycharging == false)
                            invalidate();
                        batterycharging=true;
                    } else {
                        // Battery discharging
                        if (batterycharging)
                            invalidate();
                        batterycharging=false;
                    }

                    // Error checking that probably isn't needed but I added just in case.
                    if(level == -1 || scale == -1)
                        batteryfill = 50.0f;
                    else
                        batteryfill = (float)level / (float)scale * 100.0f;
                }
            },new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            LocalBroadcastManager.getInstance(AnalogWatchFaceService.this).registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String msg = intent.getStringExtra("message");
                    if (msg.equals("START")) {
                        mTime.setToNow();
                        mTimer=mTime.toMillis(true);
                    } else if (msg.equals("STOP")) {
                        mTimer = 0;
                    } else if (msg.equals("CTDSTART")) {
                        long delay = (long)intent.getIntExtra("TIME",0)*1000l;
                        if (delay > 0) {
                            mTime.setToNow();
                            mCountdown = mTime.toMillis(true) + delay;
                            cntDiffdefined = false;
                        }
                    } else if (msg.equals("CTDSTOP")) {
                        cntDiffdefined = false;
                        mCountdown = 0;
                    } else if (msg.equals("SHOWTIMERHAND")) {
                        mTimerHand=intent.getBooleanExtra("SHOW",false);
                    } else if (msg.equals("SHOWCOUNTDOWNHAND")) {
                        mCountdownHand=intent.getBooleanExtra("SHOW",false);
                    } else if (msg.equals("STATUS")) {
                        // Send status
                        Intent tx = new Intent("AlphaDracoCFG");
                        tx.putExtra("message","STATUS");
                        tx.putExtra("TIMER",(boolean)(mTimer > 0));
                        tx.putExtra("TIMERHAND",mTimerHand);
                        tx.putExtra("COUNTDOWN",(boolean)(mCountdown > 0));
                        tx.putExtra("COUNTDOWNHAND",mCountdownHand);
                        LocalBroadcastManager.getInstance(AnalogWatchFaceService.this).sendBroadcast(tx);
                    }
                }
            },new IntentFilter("AlphaDracoTMR"));

            setWatchFaceStyle(new WatchFaceStyle.Builder(AnalogWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = AnalogWatchFaceService.this.getResources();
            // Drawable backgroundDrawable = resources.getDrawable(com.alphadraco.watchface.analog.R.drawable.bg);
            // mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            boolean ambient_alias = true;

            // Hands
            mHourPaint = new Paint();
            mHourPaint.setARGB(255, 80, 80, 80);
            mHourPaint.setStrokeWidth(6.f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mHourPaint_ambient = new Paint();
            mHourPaint_ambient.setARGB(255, 80, 80, 80);
            mHourPaint_ambient.setStrokeWidth(6.f);
            mHourPaint_ambient.setAntiAlias(ambient_alias);
            mHourPaint_ambient.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setARGB(255, 130, 130, 130);
            mMinutePaint.setStrokeWidth(6.f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint_ambient = new Paint();
            mMinutePaint_ambient.setARGB(255, 130, 130, 130);
            mMinutePaint_ambient.setStrokeWidth(6.f);
            mMinutePaint_ambient.setAntiAlias(ambient_alias);
            mMinutePaint_ambient.setStrokeCap(Paint.Cap.ROUND);

            mMinuteIndicatorPaint = new Paint();
            mMinuteIndicatorPaint.setARGB(255, 0, 200, 0);
            mMinuteIndicatorPaint.setStrokeWidth(2.f);
            mMinuteIndicatorPaint.setAntiAlias(true);
            mMinuteIndicatorPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinuteIndicatorPaint_ambient = new Paint();
            mMinuteIndicatorPaint_ambient.setARGB(255, 0, 200, 0);
            mMinuteIndicatorPaint_ambient.setStrokeWidth(1.f);
            mMinuteIndicatorPaint_ambient.setAntiAlias(ambient_alias);
            mMinuteIndicatorPaint_ambient.setStrokeCap(Paint.Cap.ROUND);

            mHourIndicatorPaint = new Paint();
            mHourIndicatorPaint.setARGB(255, 200, 0, 0);
            mHourIndicatorPaint.setStrokeWidth(2.f);
            mHourIndicatorPaint.setAntiAlias(true);
            mHourIndicatorPaint.setStrokeCap(Paint.Cap.ROUND);

            mHourIndicatorPaint_ambient = new Paint();
            mHourIndicatorPaint_ambient.setARGB(255, 200, 0, 0);
            mHourIndicatorPaint_ambient.setStrokeWidth(1.f);
            mHourIndicatorPaint_ambient.setAntiAlias(ambient_alias);
            mHourIndicatorPaint_ambient.setStrokeCap(Paint.Cap.ROUND);

            mSecondPaint = new Paint();
            mSecondPaint.setARGB(255, 255, 0, 0);
            mSecondPaint.setStrokeWidth(2.f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);

            // Auxiliary Hands
            mTimerHandPaint = new Paint();
            mTimerHandPaint.setARGB(255, 255, 0, 0);
            mTimerHandPaint.setStrokeWidth(2.f);
            mTimerHandPaint.setAntiAlias(true);
            // mTimerHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mTimerHandPaint_ambient = new Paint();
            mTimerHandPaint_ambient.setARGB(255, 255, 0, 0);
            mTimerHandPaint_ambient.setStrokeWidth(1.f);
            mTimerHandPaint_ambient.setAntiAlias(ambient_alias);
            //mTimerHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mCountdownHandPaint = new Paint();
            mCountdownHandPaint.setARGB(255, 0, 255, 0);
            mCountdownHandPaint.setStrokeWidth(2.f);
            mCountdownHandPaint.setAntiAlias(true);

            mCountdownHandPaint_ambient = new Paint();
            mCountdownHandPaint_ambient.setARGB(255, 0, 255, 0);
            mCountdownHandPaint_ambient.setStrokeWidth(1.f);
            mCountdownHandPaint_ambient.setAntiAlias(ambient_alias);

            // Zero/noon Tick
            mZeroTickPaint = new Paint();
            mZeroTickPaint.setARGB(255, 255, 0, 0);
            mZeroTickPaint.setStrokeWidth(1.f);
            mZeroTickPaint.setAntiAlias(true);
            mZeroTickPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mZeroTickPaint_ambient = new Paint();
            mZeroTickPaint_ambient.setARGB(255, 127, 0, 0);
            mZeroTickPaint_ambient.setStrokeWidth(1.f);
            mZeroTickPaint_ambient.setAntiAlias(ambient_alias);
            mZeroTickPaint_ambient.setStyle(Paint.Style.STROKE);

            // Normal Hour Tick
            mHourTickPaint = new Paint();
            mHourTickPaint.setARGB(255, 0, 255, 0);
            mHourTickPaint.setStrokeWidth(1.f);
            mHourTickPaint.setAntiAlias(true);
            mHourTickPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mHourTickPaint_ambient = new Paint();
            mHourTickPaint_ambient.setARGB(255, 0, 127, 0);
            mHourTickPaint_ambient.setStrokeWidth(1.f);
            mHourTickPaint_ambient.setAntiAlias(ambient_alias);
            mHourTickPaint_ambient.setStyle(Paint.Style.STROKE);

            // Normal Tick
            mMinuteTickPaint = new Paint();
            mMinuteTickPaint.setARGB(255, 0, 127, 0);
            mMinuteTickPaint.setStrokeWidth(1.f);
            mMinuteTickPaint.setAntiAlias(true);
            mMinuteTickPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mMinuteTickPaint_ambient = new Paint();
            mMinuteTickPaint_ambient.setARGB(255, 0, 63, 0);
            mMinuteTickPaint_ambient.setStrokeWidth(1.f);
            mMinuteTickPaint_ambient.setAntiAlias(ambient_alias);
            mMinuteTickPaint_ambient.setStyle(Paint.Style.STROKE);

            mCenterPaint = new Paint();
            mCenterPaint.setARGB(255, 200, 200, 200);
            mCenterPaint.setStrokeWidth(1.f);
            mCenterPaint.setAntiAlias(true);
            mCenterPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mCenterPaint_ambient = new Paint();
            mCenterPaint_ambient.setARGB(255, 200, 200, 200);
            mCenterPaint_ambient.setStrokeWidth(1.f);
            mCenterPaint_ambient.setAntiAlias(ambient_alias);
            mCenterPaint_ambient.setStyle(Paint.Style.FILL_AND_STROKE);

            // Bold Text
            mBoldTextPaint = new Paint();
            mBoldTextPaint.setARGB(255, 127, 127, 127);
            mBoldTextPaint.setTypeface(Typeface.create("Droid Sans", Typeface.BOLD));
            mBoldTextPaint.setTextSize(20f);
            mBoldTextPaint.setTextAlign(Paint.Align.CENTER);
            mBoldTextPaint.setAntiAlias(true);

            mBoldTextPaint_ambient = new Paint();
            mBoldTextPaint_ambient.setARGB(255, 63, 63, 63);
            mBoldTextPaint_ambient.setTypeface(Typeface.create("Droid Sans", Typeface.BOLD));
            mBoldTextPaint_ambient.setTextSize(20f);
            mBoldTextPaint_ambient.setTextAlign(Paint.Align.CENTER);
            mBoldTextPaint_ambient.setAntiAlias(ambient_alias);

            // Normal Text
            mNormalTextPaint = new Paint();
            mNormalTextPaint.setARGB(255, 127, 127, 127);
            mNormalTextPaint.setTypeface(Typeface.create("Droid Sans", Typeface.NORMAL));
            mNormalTextPaint.setTextSize(20f);
            mNormalTextPaint.setTextAlign(Paint.Align.CENTER);
            mNormalTextPaint.setAntiAlias(true);

            mNormalTextPaint_ambient = new Paint();
            mNormalTextPaint_ambient.setARGB(255, 63, 63, 63);
            mNormalTextPaint_ambient.setTypeface(Typeface.create("Droid Sans", Typeface.NORMAL));
            mNormalTextPaint_ambient.setTextSize(20f);
            mNormalTextPaint_ambient.setTextAlign(Paint.Align.CENTER);
            mNormalTextPaint_ambient.setAntiAlias(ambient_alias);

            // Normal Red Text
            mNormalRedTextPaint = new Paint();
            mNormalRedTextPaint.setARGB(255, 192, 0, 0);
            mNormalRedTextPaint.setTypeface(Typeface.create("Droid Sans", Typeface.NORMAL));
            mNormalRedTextPaint.setTextSize(20f);
            mNormalRedTextPaint.setTextAlign(Paint.Align.CENTER);
            mNormalRedTextPaint.setAntiAlias(true);

            mNormalRedTextPaint_ambient = new Paint();
            mNormalRedTextPaint_ambient.setARGB(255, 127, 0, 0);
            mNormalRedTextPaint_ambient.setTypeface(Typeface.create("Droid Sans", Typeface.NORMAL));
            mNormalRedTextPaint_ambient.setTextSize(20f);
            mNormalRedTextPaint_ambient.setTextAlign(Paint.Align.CENTER);
            mNormalRedTextPaint_ambient.setAntiAlias(ambient_alias);

            mTime = new Time();
            mTime.switchTimezone(TimeZone.getDefault().getID());
            //Calendar cal = Calendar.getInstance();
            //TimeZone tz = cal.getTimeZone();
            //mTime.switchTimezone(tz.getID());
            mTimer = 0; //new Time();
            // mTimer.setToNow();

            //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            //mTemperature = mSensorManager.getDefaultSensor(Sensor.TYPE_TEMPERATURE);
            //mSensorManager.registerListener(this, mTemperature, SensorManager.SENSOR_DELAY_NORMAL);

        }

        //SensorManager mSensorManager;
        //Sensor mTemperature;


        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            //mSensorManager.unregisterListener(this);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onPropertiesChanged: low-bit ambient = " + mLowBitAmbient);
            }
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onTimeTick: ambient = " + isInAmbientMode());
            }
            invalidate();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);
            }
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mSecondPaint.setAntiAlias(antiAlias);

            }
            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);
            if (mMute != inMuteMode) {
                mMute = inMuteMode;
                //mHourPaint.setAlpha(inMuteMode ? 100 : 255);
                //mMinutePaint.setAlpha(inMuteMode ? 100 : 255);
                //mSecondPaint.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        Bitmap Bitmap_ambient, Bitmap_normal;

        public void drawBackground(Canvas drawCanvas, Rect bounds, boolean ambient) {
            // Check for cached version
            if (ambient) {
                if (Bitmap_ambient != null) {
                    drawCanvas.drawBitmap(Bitmap_ambient, 0, 0, null);
                    return;
                }
            } else {
                if (Bitmap_normal != null) {
                    drawCanvas.drawBitmap(Bitmap_normal, 0, 0, null);
                    return;
                }
            }
            // Needs new drawing
            //int width = bounds.width();
            //int height = bounds.height();
            // DisplayMetrics metrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics metrics = new DisplayMetrics();
            windowManager.getDefaultDisplay().getMetrics(metrics);
            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            Bitmap Btemp = Bitmap.createBitmap(metrics, width ,height, Bitmap.Config.ARGB_8888);
            float centerX = width / 2f;
            float centerY = height / 2f;
            Canvas canvas = new Canvas(Btemp);

            // Clear
            canvas.drawARGB(255,0,0,0); // Clear
            // Draw the background, scaled to fit.
            /*if (mBackgroundScaledBitmap == null
                    || mBackgroundScaledBitmap.getWidth() != width
                    || mBackgroundScaledBitmap.getHeight() != height) {
                mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true);
            }
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
            */

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.

            // Draw the ticks.
            float clockrad = centerX-2f;
            if (ambient)
                clockrad -= 6f;

            for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                float tickRot;
                tickRot = (float) (tickIndex * Math.PI * 2 / 60);

                if (tickIndex % 5 == 0) {
                    // Hours
                    float ofsx, ofsy;
                    float innerX, innerY;
                    float outerX, outerY;
                    outerX = (float) Math.sin(tickRot) * clockrad;
                    outerY = (float) -Math.cos(tickRot) * clockrad;
                    if ((tickIndex/5) % 3 == 0) {
                        // 0/3/6/9
                        ofsx=(float) Math.cos(tickRot)*2.5f;
                        ofsy=(float) Math.sin(tickRot)*2.5f;
                        innerX = (float) Math.sin(tickRot) * (clockrad-20f);
                        innerY = (float) -Math.cos(tickRot) * (clockrad-20f);
                    } else {
                        ofsx=(float) Math.cos(tickRot)*1.5f;
                        ofsy=(float) Math.sin(tickRot)*1.5f;
                        innerX = (float) Math.sin(tickRot) * (clockrad-15f);
                        innerY = (float) -Math.cos(tickRot) * (clockrad-15f);
                    }
                    Path poly = new Path();
                    poly.moveTo(centerX + innerX-ofsx, centerY + innerY-ofsy);
                    poly.lineTo(centerX + outerX-ofsx, centerY + outerY-ofsy);
                    poly.lineTo(centerX + outerX+ofsx, centerY + outerY+ofsy);
                    poly.lineTo(centerX + innerX+ofsx, centerY + innerY+ofsy);
                    poly.close();

                    Paint pnt;
                    if (tickIndex == 0) {
                        if (ambient)
                            pnt = mZeroTickPaint_ambient;
                        else
                            pnt = mZeroTickPaint;
                    } else {
                        if (ambient)
                            pnt = mHourTickPaint_ambient;
                        else
                            pnt = mHourTickPaint;
                    }
                    canvas.drawPath(poly,pnt);
                    //canvas.drawLine(centerX + innerX-ofsx, centerY + innerY-ofsy,
                    //        centerX + outerX-ofsx, centerY + outerY-ofsy, pnt);
                    //canvas.drawLine(centerX + innerX+ofsx, centerY + innerY+ofsy,
                    //        centerX + outerX+ofsx, centerY + outerY+ofsy, pnt);
                } else {
                    // Minutes
                    if (!ambient) {
                        float posx = (float) Math.sin(tickRot) * (clockrad - 7.5f);
                        float posy = (float) Math.cos(tickRot) * (clockrad - 7.5f);
                        Paint pnt;
                        if (ambient)
                            pnt = mMinuteTickPaint_ambient;
                        else
                            pnt = mMinuteTickPaint;
                        canvas.drawCircle(centerX + posx, centerY + posy, 2.5f, pnt);
                    }
                }
            }

            drawCanvas.drawBitmap(Btemp, 0, 0, null);
            if (ambient) {
                Bitmap_ambient = Btemp;
            } else {
                Bitmap_normal = Btemp;
            }
        }

        private void drawHand(Canvas canvas, float cx, float cy, float phi, float rad1_out, float rad1_in, Paint rad1_paint, float rad2_out, float rad2_in, Paint rad2_paint) {
            float sp = (float) Math.sin(phi);
            float cp = (float) - Math.cos(phi);
            float minX = sp * rad1_in;
            float minY = cp * rad1_in;
            float maxX = sp * rad1_out;
            float maxY = cp * rad1_out;
            if (rad1_paint != null)
                canvas.drawLine(cx+minX,cy+minY,cx+maxX,cy+maxY,rad1_paint);
            minX = sp * rad2_in;
            minY = cp * rad2_in;
            maxX = sp * rad2_out;
            maxY = cp * rad2_out;
            if (rad2_paint != null)
                canvas.drawLine(cx+minX,cy+minY,cx+maxX,cy+maxY,rad2_paint);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();
            long now = mTime.toMillis(true);

            boolean ambient = isInAmbientMode();
            // ambient = ((mTime.second/5) % 2) == 0;

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background (Ticks, frame... whatever)
            // This function also caches the resulting image so that updates run much faster
            drawBackground(canvas,bounds,ambient);

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;
            String s;

            // Text Displays
            // Date and Work Week
            if (!ambient) {
                s = mTime.format("%a, %d.%m.%Y, KW %V");
                if (ambient)
                    canvas.drawText(s, centerX, centerY + 30f, mNormalTextPaint_ambient);
                else
                    canvas.drawText(s, centerX, centerY + 30f, mNormalTextPaint);
            }

            // Primary Timezone
            if (!ambient)
                s = mTime.format("%H:%M:%S");
            else
                s = mTime.format("%H:%M");
            if (ambient)
                canvas.drawText(s,centerX,centerY+60f,mBoldTextPaint_ambient);
            else
                canvas.drawText(s,centerX,centerY+60f,mBoldTextPaint);

            // Second Time Zone
            if (!ambient) {
                Time m2Time = new Time(mTime);
                m2Time.switchTimezone("America/New_York");
                if (m2Time.hour < 12)
                    s = m2Time.format("Pgh %l:%M ") + "AM";
                else
                    s = m2Time.format("Pgh %l:%M ") + "PM";
                if (ambient)
                    canvas.drawText(s, centerX, centerY + 90f, mNormalTextPaint_ambient);
                else
                    canvas.drawText(s, centerX, centerY + 90f, mNormalTextPaint);
            }

            // Timers if active
            int tmrDiff=0;
            if (mTimer > 0) {
                tmrDiff = (int) ((long) (now - mTimer) / 1000l); // in s
                int hours = tmrDiff / (60 * 60);
                int minutes = (tmrDiff - hours * (60 * 60)) / 60;
                int seconds = (tmrDiff - hours * (60 * 60) - minutes * 60);
                if (ambient)
                    s = String.format("T %02d:%02d",hours,minutes);
                else
                    s = String.format("T %02d:%02d:%02d",hours,minutes,seconds);
                if (ambient)
                    canvas.drawText(s, centerX, centerY - 30f, mNormalTextPaint_ambient);
                else
                    canvas.drawText(s, centerX, centerY - 30f, mNormalTextPaint);
            }

            int cntDiff=0;
            boolean cntNeg=false;
            if (mCountdown > 0) {
                cntDiff = (int) ((long) (mCountdown-now) / 1000l); // in s
                if (cntDiffdefined && (cntDiffold > 0) && (cntDiff <= 0)) {
                    Vibrator vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
                    vibrator.vibrate(500);
                }
                cntDiffold = cntDiff;
                cntDiffdefined = true;
                cntNeg = (cntDiff < 0);
                if (cntNeg) cntDiff = -cntDiff;
                int hours = cntDiff / (60 * 60);
                int minutes = (cntDiff - hours * (60 * 60)) / 60;
                int seconds = (cntDiff - hours * (60 * 60) - minutes * 60);
                if (ambient)
                    s = String.format("C %02d:%02d",hours,minutes);
                else
                    s = String.format("C %02d:%02d:%02d",hours,minutes,seconds);
                float pos = 30f;
                if (mTimer > 0) pos=60f;
                if (cntNeg) {
                    if (ambient)
                        canvas.drawText(s, centerX, centerY - pos, mNormalRedTextPaint_ambient);
                    else
                        canvas.drawText(s, centerX, centerY - pos, mNormalRedTextPaint);
                } else {
                    if (ambient)
                        canvas.drawText(s, centerX, centerY - pos, mNormalTextPaint_ambient);
                    else
                        canvas.drawText(s, centerX, centerY - pos, mNormalTextPaint);
                }
            }

            // Battery State
            // s = String.format("%3.0f %%, %2.1f °C", batteryfill, temperature);
            if (batterycharging)
                s = String.format("++ %3.0f %%", batteryfill);
            else
                s = String.format("%3.0f %%", batteryfill);
            if ((mTimer > 0) && (mCountdown > 0)) {
                if (ambient)
                    canvas.drawText(s, centerX, centerY - 90f, mNormalTextPaint_ambient);
                else
                    canvas.drawText(s, centerX, centerY - 90f, mNormalTextPaint);
            } else {
                if (ambient)
                    canvas.drawText(s, centerX, centerY - 60f, mNormalTextPaint_ambient);
                else
                    canvas.drawText(s, centerX, centerY - 60f, mNormalTextPaint);
            }

            // Hands
            float clockrad = centerX-2f;
            if (ambient)
                clockrad -= 6f;

            // Timer/Countdown Hands
            if ((mTimer > 0) && mTimerHand) {
                float tmrRot;
                boolean longhand=false;
                if (tmrDiff < 60*60) {
                    // Below one Hour
                    int minutes = tmrDiff / 60;
                    tmrRot=(float) minutes/(60f)*2f*(float)Math.PI;
                    longhand=true;
                } else {
                    // Above one hour
                    tmrRot=(float) tmrDiff/(3600f*12f)*2f*(float)Math.PI;
                }
                float rend = (longhand)?(clockrad - 30f):(clockrad - 70f);
                float rstart = 60f;
                float sn = (float) Math.sin(tmrRot);
                float sc = (float) -Math.cos(tmrRot);
                float tm1X = sn * rstart;
                float tm1Y = sc * rstart;
                float tm2X = sn * rend;
                float tm2Y = sc * rend;
                if (ambient)
                    canvas.drawLine(centerX+tm1X, centerY+tm1Y, centerX + tm2X, centerY + tm2Y, mTimerHandPaint_ambient);
                else
                    canvas.drawLine(centerX+tm1X, centerY+tm1Y, centerX + tm2X, centerY + tm2Y, mTimerHandPaint);
            }

            if ((mCountdown > 0) && mCountdownHand) {
                float tmrRot;
                boolean longhand=false;
                if (cntDiff < 60*60) {
                    // Below one Hour
                    int minutes = cntDiff / 60;
                    tmrRot=(float) minutes/(60f)*2f*(float)Math.PI;
                    longhand=true;
                } else {
                    // Above one hour
                    tmrRot=(float) cntDiff/(3600f*12f)*2f*(float)Math.PI;
                }
                if (cntNeg) tmrRot = -tmrRot;
                float rend = (longhand)?(clockrad - 30f):(clockrad - 70f);
                float rstart = 60f;
                float sn = (float) -Math.sin(tmrRot);
                float sc = (float) -Math.cos(tmrRot);
                float tm1X = sn * rstart;
                float tm1Y = sc * rstart;
                float tm2X = sn * rend;
                float tm2Y = sc * rend;
                if (ambient)
                    canvas.drawLine(centerX+tm1X, centerY+tm1Y, centerX + tm2X, centerY + tm2Y, mCountdownHandPaint_ambient);
                else
                    canvas.drawLine(centerX+tm1X, centerY+tm1Y, centerX + tm2X, centerY + tm2Y, mCountdownHandPaint);
            }

            // Standard Watch Hands
            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f ) * (float) Math.PI;

            // Minute Hand
            if (ambient)
                drawHand(canvas, centerX, centerY, minRot,
                        clockrad - 40f,-20f, mMinutePaint_ambient,
                        clockrad - 42f,clockrad - 42f - 40f, mMinuteIndicatorPaint_ambient);
            else
                drawHand(canvas, centerX, centerY, minRot,
                        clockrad - 40f,-20f, mMinutePaint,
                        clockrad - 42f,clockrad - 42f - 40f, mMinuteIndicatorPaint);

            // Hour Hand
            if (ambient)
                drawHand(canvas, centerX, centerY, hrRot,
                        clockrad - 80f,-20f, mHourPaint_ambient,
                        clockrad - 82f,clockrad - 82f - 30f, mHourIndicatorPaint_ambient);
            else
                drawHand(canvas, centerX, centerY, hrRot,
                        clockrad - 80f,-20f, mHourPaint,
                        clockrad - 82f,clockrad - 82f - 30f, mHourIndicatorPaint);

            // Second Hand
            if (!ambient) {
                float secX = (float) Math.sin(secRot) * (clockrad - 30f);
                float secY = (float) -Math.cos(secRot) * (clockrad - 30f);
                canvas.drawLine(centerX, centerY, centerX + secX, centerY + secY, mSecondPaint);
                canvas.drawOval(centerX+secX-5f,centerY+secY-5f,centerX+secX+5f,centerY+secY+5f,mSecondPaint);
            }

            // Center
            Paint pnt;
            if (ambient)
                pnt = mCenterPaint_ambient;
            else
                pnt = mCenterPaint;
            canvas.drawCircle(centerX, centerY,4f,pnt);

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onVisibilityChanged: " + visible);
            }

            if (visible) {
                //mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();

                /*if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }*/
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }


        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            AnalogWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            AnalogWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "updateTimer");
            }
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

    }
}
