package com.crimsonsky.fitwatch.app;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.os.Process;

/**
 * Author: Ajay Thampi
 */

public class StepCounterService extends Service {
    public static final String TODAYS_DATE = "todaysdate";
    public static final String WATCH = "watch";
    public static final String STEPS_COUNT = "stepscount";
    public static final String NOTIFICATION = "com.crimsonsky.fitwatch.app";
    public static final int NOTIFICATION_ID = 0;
    public static final int SMOOTHING_MAG = 3;
    public static final int SMOOTHING_DIFF = 50;
    public static final double THRESHOLD_MIN = 0.15;
     //public static final String LOG_FILE_PREFIX = "FitWatch_";

    private ServiceHandler mServiceHandler;
    private int mStepsCount = 0;
    private String mTodaysDate = null;
    private boolean watch = false;

    /*private float mLastValues[] = new float[3*2];
    private float mScale[] = new float[2];
    private float mYOffset;

    private float mLastDirections[] = new float[3*2];
    private float mLastExtremes[][] = { new float[3*2], new float[3*2] };
    private float mLastDiff[] = new float[3*2];
    private int mLastMatch = -1;*/

    private double mAvgAccMagnitude = 0;
    private double mPrevAvgAccMagnitude = 0;
    private int mSmoothingMagIndex = 0;
    private int mSmoothingDiffIndex = 0;
    private double mStepsThreshold = 0;
    private double mDiff = 0;
    private double mSmoothingDiffWindow = SMOOTHING_DIFF;
    // private BufferedWriter logWriter = null;

    // Handler that receives messages from the main thread
    private final class ServiceHandler extends Handler implements SensorEventListener {
        private final SensorManager mSensorManager;
        private final Sensor mAccelerometer;

        public ServiceHandler(Looper looper) {
            super(looper);
            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            /*int h = 480;
            mYOffset = h * 0.5f;
            mScale[0] = - (h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
            mScale[1] = - (h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));*/
        }

        @Override
        public void handleMessage(Message msg) {
            String command = (String) msg.obj;
            if (command == null)
                return;

            if (command.contains(TODAYS_DATE)) {
                // Reset steps count if the date has changed
                if ((mTodaysDate == null)||(!mTodaysDate.equals(command))) {
                    reset();
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

        private void reset() {
            mStepsCount = 0;
            mAvgAccMagnitude = 0;
            mPrevAvgAccMagnitude = 0;
            mSmoothingMagIndex = 0;
            mSmoothingDiffIndex = 0;
            mStepsThreshold = 0;
            mDiff = 0;
            mSmoothingDiffWindow = SMOOTHING_DIFF;
        }

        private void sendNotification() {
            /* // Send notifications to watch
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
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());*/

            /*// Create log file
            long currentTime = System.currentTimeMillis();
            try
            {
                File fitWatchFolder = new File(Environment.getExternalStorageDirectory() + "/FitWatch");
                if (!fitWatchFolder.exists()) {
                    fitWatchFolder.mkdir();
                }
                String fitWatchFilename = Environment.getExternalStorageDirectory() + "/FitWatch/" +
                                            LOG_FILE_PREFIX + Long.toString(currentTime) + ".csv";
                File fitWatchFile = new File(fitWatchFilename);
                fitWatchFile.createNewFile();
                logWriter = new BufferedWriter(new FileWriter(fitWatchFilename));
                if (logWriter != null) {
                    Log.d("FITWATCH","Created file " + logWriter.toString());
                }
            } catch (IOException e) {}*/
        }

        private void cancelNotification() {
            /*NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(getApplicationContext());
            notificationManager.cancel(NOTIFICATION_ID);*/

            /*// Close log file
            try {
                if (logWriter != null)
                    logWriter.close();
            } catch (IOException e) {}
            logWriter = null;*/
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        public void onSensorChanged(SensorEvent event) {
            Sensor sensor = event.sensor;
            synchronized (this) {
                if (sensor.getType() != Sensor.TYPE_ACCELEROMETER)
                    return;

                long currentTime = System.currentTimeMillis();
                // String log = "";
                if (mSmoothingMagIndex >= SMOOTHING_MAG) {
                    if (mPrevAvgAccMagnitude != 0) {
                        mDiff = Math.abs(mAvgAccMagnitude - mPrevAvgAccMagnitude);
                        mStepsThreshold += mDiff / SMOOTHING_DIFF;
                        mSmoothingDiffIndex += 1;

                        if (mSmoothingDiffIndex >= mSmoothingDiffWindow) {
                            mSmoothingDiffWindow = 0;
                            mSmoothingDiffIndex = 0;
                            if ((mDiff > mStepsThreshold) && (mDiff > THRESHOLD_MIN)) {
                                mStepsCount += 1;
                                Intent intent = new Intent(NOTIFICATION);
                                intent.putExtra(STEPS_COUNT, Integer.toString(mStepsCount));
                                sendBroadcast(intent);
                            }
                        }

                        /*log = Long.toString(currentTime) +
                                ",0,0,0,0," +
                                Double.toString(mAvgAccMagnitude) + "," +
                                Double.toString(mPrevAvgAccMagnitude) + "," +
                                Double.toString(diff) + "\n";
                        if (logWriter != null) {
                            try {
                                logWriter.append(log);
                            } catch (IOException e) {}
                        }*/

                        /*if (diff > THRESHOLD) {
                            mStepsCount += 1;
                            Intent intent = new Intent(NOTIFICATION);
                            intent.putExtra(STEPS_COUNT, Integer.toString(mStepsCount));
                            sendBroadcast(intent);
                        }*/
                    }
                    mPrevAvgAccMagnitude = mAvgAccMagnitude;
                    mAvgAccMagnitude = 0;
                    mSmoothingMagIndex = 0;
                }

                double accMagnitude = Math.sqrt(Math.pow(event.values[0],2) +
                                                Math.pow(event.values[1],2) +
                                                Math.pow(event.values[2],2));
                mAvgAccMagnitude = mAvgAccMagnitude + (accMagnitude / SMOOTHING_MAG);
                mSmoothingMagIndex += 1;

                    /*log = Long.toString(currentTime) + "," +
                            Float.toString(event.values[0]) + "," +
                            Float.toString(event.values[1]) + "," +
                            Float.toString(event.values[2]) + "," +
                            Double.toString(accMagnitude) + "," +
                            "0,0,0\n";
                    if (logWriter != null) {
                        try {
                            logWriter.append(log);
                        } catch (IOException e) {}
                    }*/
            }

            /*Sensor sensor = event.sensor;
            synchronized (this) {
                int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1 : 0;
                float limit = (float) 1.97; // 1.97  2.96  4.44  6.66  10.00  15.00  22.50  33.75  50.62
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
                        int extType = (direction > 0 ? 0 : 1); // minimum or maximum?
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
            }*/
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
