package com.ashu.sensorstest.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ashu.sensorstest.MainActivity;
import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.Data_for_graphsDBHelper;
import com.ashu.sensorstest.sensors.Sensors;
import com.ashu.sensorstest.string_builders.StringBuilders;


public class MainService extends Service {

    private SensorManager sensorManagerService;
    private Sensor sensorGyroscope;
    private Sensor sensorAccelerometer;
    ArrayList<String> arr_valuesGyroscope = new ArrayList<>();
    ArrayList<String> arr_valuesAcceleration = new ArrayList<>();

    private NotificationManager notificationManager;
    public static final int DEFAULT_NOTIFICATION_ID = 101;
    PowerManager.WakeLock mWakeLock;

    private ScheduledExecutorService schedule_Data_Processing;
    private ScheduledExecutorService schedule_Cleaning_of_databases;

    private Data_for_graphsDBHelper mDbHelper_Graphs = new Data_for_graphsDBHelper(this);

    private Sensors mSensors = new Sensors();
    private StringBuilders mStringBuilder = new StringBuilders();

    public void onCreate() {
        super.onCreate();
        sensorManagerService = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManagerService != null) {
            sensorGyroscope = sensorManagerService.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorAccelerometer = sensorManagerService.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {

        if (sensorManagerService != null) {
            sensorManagerService.registerListener(listener, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManagerService.registerListener(listener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Data_Processing();
            Cleaning_of_databases();
        }

        //Send Foreground Notification
        String text_fo_Notification_Up = getResources().getString(R.string.text_for_Notification) + ":";
        String text_fo_Notification_Down = " '"
                + getResources().getString(R.string.ACCELERATION) + "' и '"
                + getResources().getString(R.string.GYROSCOPE) + "'";
        sendNotification(text_fo_Notification_Up, text_fo_Notification_Down);

        return START_REDELIVER_INTENT;
    }

    public void onDestroy() {

        if (sensorManagerService != null) {
            sensorManagerService.unregisterListener(listener);
        }
        schedule_Data_Processing.shutdownNow();
        schedule_Cleaning_of_databases.shutdownNow();

        mDbHelper_Graphs.Close_DB_for_graphs();
        mDbHelper_Graphs.close();

        //Removing any notifications
        notificationManager.cancel(DEFAULT_NOTIFICATION_ID);

        //Disabling service
        stopSelf();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        return null;
    }


    private SensorEventListener listener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {

            float[] valuesGyroscope = new float[3];
            float[] valuesAcceleration = new float[3];
            String finished_line;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(event.values, 0, valuesGyroscope, 0, 3);
                    finished_line = mStringBuilder.BS_Data_from_sensors(valuesGyroscope);
                    arr_valuesGyroscope.add(finished_line);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.arraycopy(event.values, 0, valuesAcceleration, 0, 3);
                    finished_line = mStringBuilder.BS_Data_from_sensors(valuesAcceleration);
                    arr_valuesAcceleration.add(finished_line);
                    break;
            }
        }
    };

    private void Data_Processing() {

        schedule_Data_Processing = Executors.newScheduledThreadPool(1);
        schedule_Data_Processing.scheduleAtFixedRate(new Runnable() {
            public void run() {
                acquireWakeLock();

                ArrayList<String> Buffer_1 = new ArrayList<>(arr_valuesGyroscope);
                ArrayList<String> Buffer_2 = new ArrayList<>( arr_valuesAcceleration);
                arr_valuesGyroscope.clear();
                arr_valuesAcceleration.clear();
                Processing_of_data_from_sensors(Buffer_1, Buffer_2);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }


    private void Processing_of_data_from_sensors(ArrayList<String> buffer_1, ArrayList<String> buffer_2) {

        long timenow = TakeTimeNow();
        String row_fot_writing;

        String sensor_type = getResources().getString(R.string.GYROSCOPE);
        row_fot_writing = mSensors.Gyroscope(buffer_1);
        mDbHelper_Graphs.Recording_Data_for_graphs(sensor_type, timenow, row_fot_writing);

        sensor_type = getResources().getString(R.string.ACCELERATION);
        row_fot_writing = mSensors.Acceleration(buffer_2);
        mDbHelper_Graphs.Recording_Data_for_graphs(sensor_type, timenow, row_fot_writing);
    }

    private void Cleaning_of_databases(){

        schedule_Cleaning_of_databases = Executors.newScheduledThreadPool(1);
        schedule_Cleaning_of_databases.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Cleaner_DB();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void Cleaner_DB(){
        long timenow = TakeTimeNow();
        int Interval_Step_for_graphs = 360000; //в миллисекундах. За какой промежуток будем делать выборку
        mDbHelper_Graphs.Clear_DB_for_graphs(timenow, Interval_Step_for_graphs);
    }

    private long TakeTimeNow(){
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    public void sendNotification(String Title, String Text) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "my_channel_id_01");
        builder.setContentIntent(contentIntent)
                .setOngoing(true)   //Запретить "смахивание"
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(Title) //Заголовок
                .setContentText(Text) // Текст уведомления
                .setWhen(System.currentTimeMillis());

        Notification notification;
        notification = builder.build();

        startForeground(DEFAULT_NOTIFICATION_ID, notification);
    }

    public void acquireWakeLock() {
        final PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        releaseWakeLock();
        //Acquire new wake lock
        if (powerManager != null) {
            mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PARTIAL_WAKE_LOCK");
            mWakeLock.acquire(60*1000L /*1 minute*/);
        }

    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
