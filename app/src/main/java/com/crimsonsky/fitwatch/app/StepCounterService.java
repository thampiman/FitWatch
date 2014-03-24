package com.crimsonsky.fitwatch.app;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.Process;
import android.preview.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.NotificationCompat;

/**
 * Author: Ajay Thampi
 */

public class StepCounterService extends Service {
    public static final String TODAYS_DATE = "todaysdate";
    public static final String WATCH = "watch";
    public static final String STEPS_COUNT = "stepscount";
    public static final String NOTIFICATION = "com.crimsonsky.fitwatch.app";
    public static final int NOTIFICATION_ID = 0;

    private ServiceHandler mServiceHandler;
    private int mStepsCount;
    private String mTodaysDate = null;
    private boolean watch = false;

    private float mLastValues[] = new float[3*2];
    private float mScale[] = new float[2];
    private float mYOffset;

    private float mLastDirections[] = new float[3*2];
    private float mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float mLastDiff[] = new float[3*2];
    private int mLastMatch = -1;

    // Handler that receives messages from the main thread
    private final class ServiceHandler extends Handler implements SensorEventListener {
        private final SensorManager mSensorManager;
        private final Sensor mAccelerometer;

        public ServiceHandler(Looper looper) {
            super(looper);
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            int h = 480;
            mYOffset = h * 0.5f;
            mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
            mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        }

        @Override
        public void handleMessage(Message msg) {
            String command = (String) msg.obj;
            if (command == null)
                return;

            if (command.contains(TODAYS_DATE)) {
                // Reset steps count if the date has changed
                if ((mTodaysDate == null)||(!mTodaysDate.equals(command))) {
                    mStepsCount = 0;
                    mTodaysDate = command;
                }
                // Register sensor listener
                mSensorManager.registerListener(this, mAccelerometer,
                                                SensorManager.SENSOR_DELAY_NORMAL);
            } else if (command.contains(WATCH)) {
                if (!watch) {
                    sendNotification();
                    watch = true;
                } else {
                    cancelNotification();
                    watch = false;
                }
            }
        }

        private void sendNotification() {
            // Send notifications to watch
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setContentText(Integer.toString(mStepsCount) + " " +
                                    getResources().getString(R.string.steps))
                            .setSmallIcon(R.mipmap.ic_notification_fitwatch);

            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getApplicationContext());

            // Build the notification and issues it with notification manager.
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }

        private void cancelNotification() {
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getApplicationContext());
            notificationManager.cancel(NOTIFICATION_ID);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            synchronized (this) {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                float limit = 10;
                if (j == 1) {
                    float vSum = 0;
                    for (int i=0 ; i<3 ; i++) {
                        final float v = mYOffset + event.values[i] * mScale[j];
                        vSum += v;
                    }
                    int k = 0;
                    float v = vSum / 3;

                    float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1 : 0));
                    if (direction == - mLastDirections[k]) {
                        // Direction changed
                        int extType = (direction > 0 ? 0 : 1); // minumum or maximum?
                        mLastExtremes[extType][k] = mLastValues[k];
                        float diff = Math.abs(mLastExtremes[extType][k] - mLastExtremes[1 - extType][k]);

                        if (diff > limit) {

                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k]*2/3);
                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff/3);
                            boolean isNotContra = (mLastMatch != 1 - extType);

                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                mStepsCount += 1;
                                Intent intent = new Intent(NOTIFICATION);
                                intent.putExtra(STEPS_COUNT, Integer.toString(mStepsCount));
                                sendBroadcast(intent);

                                if (watch) {
                                    sendNotification();
                                }
                                mLastMatch = extType;
                            }
                            else {
                                mLastMatch = -1;
                            }
                        }
                        mLastDiff[k] = diff;
                    }
                    mLastDirections[k] = direction;
                    mLastValues[k] = v;
                }
            }
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                                                 Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Link the HandlerThread's looper with our ServiceHandler
        mServiceHandler = new ServiceHandler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get data from the main activity bundle
        if (intent != null) {
            String todaysDate = intent.getStringExtra(TODAYS_DATE);
            String watch = intent.getStringExtra(WATCH);
            String messageForServiceHandler = "";
            if (todaysDate != null) {
                messageForServiceHandler = TODAYS_DATE + ";" + todaysDate;
            } else if (watch != null) {
                messageForServiceHandler = WATCH;
            }

            // Send message to ServiceHandler
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = messageForServiceHandler;
            mServiceHandler.sendMessage(msg);
        }

        // If service gets killed after returning, then restart with pending intents
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {

    }
}
