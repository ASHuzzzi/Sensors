package com.ashu.sensorstest.services;

import android.app.AlarmManager;
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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ashu.sensorstest.MainActivity;
import com.ashu.sensorstest.R;
import com.ashu.sensorstest.data.Data_from_sensors.Data_from_sensorsDBHelper;
import com.ashu.sensorstest.data.Data_for_graphs.Data_for_graphsDBHelper;
import com.ashu.sensorstest.fragments.MainFragment;
import com.ashu.sensorstest.sensors.Sensors;
import com.ashu.sensorstest.string_builders.StringBuilders;

import static android.animation.ValueAnimator.RESTART;

public class MainService extends Service {

    private String LOG_TAG = "myLogs";

    private SensorManager sensorManagerService;
    private Sensor sensorGyroscope;
    private Sensor sensorAccelerometer;

    private ScheduledExecutorService Schedule_Data_Processing;
    private ScheduledExecutorService Schedule_Cleaning_of_databases;

    private Data_from_sensorsDBHelper mDbHelper_Sensors = new Data_from_sensorsDBHelper(this);
    private Data_for_graphsDBHelper mDbHelper_Graphs = new Data_for_graphsDBHelper(this);

    private Sensors sensors = new Sensors();
    private StringBuilders stringBuilder = new StringBuilders();

    private NotificationManager notificationManager;
    public static final int DEFAULT_NOTIFICATION_ID = 101;
    ArrayList<String> Test_Array = new ArrayList<>();;
    long Test_lasttime = 0;
    PowerManager.WakeLock mWakeLock;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
        sensorManagerService = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManagerService != null) {
            sensorGyroscope = sensorManagerService.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            sensorAccelerometer = sensorManagerService.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        }

        notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");

        if (sensorManagerService != null) {
            sensorManagerService.registerListener(listener, sensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManagerService.registerListener(listener, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            Data_Processing();
            Cleaning_of_databases();
        }

        //Send Foreground Notification
        sendNotification("Ticker","Title","Text");

        //return Service.START_STICKY;
        return START_REDELIVER_INTENT;

        //return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {

        if (sensorManagerService != null) {
            sensorManagerService.unregisterListener(listener);
        }
        Schedule_Data_Processing.shutdownNow();
        Schedule_Cleaning_of_databases.shutdownNow();

        mDbHelper_Sensors.Close_DB_from_sensors();
        mDbHelper_Sensors.close();

        mDbHelper_Graphs.Close_DB_for_graphs();
        mDbHelper_Graphs.close();
        Log.d(LOG_TAG, "onDestroy");

        //Removing any notifications
        notificationManager.cancel(DEFAULT_NOTIFICATION_ID);

        //Disabling service
        stopSelf();
        super.onDestroy();
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
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
            long timenow = TakeTimeNow();
            String finished_line;

            switch (event.sensor.getType()) {
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(event.values, 0, valuesGyroscope, 0, 3);
                    finished_line = stringBuilder.BS_Data_from_sensors(valuesGyroscope);
                    mDbHelper_Sensors.Recording_Data_From_Sensors("GYROSCOPE", timenow, finished_line);
                    break;
                case Sensor.TYPE_LINEAR_ACCELERATION:
                    System.arraycopy(event.values, 0, valuesAcceleration, 0, 3);
                    finished_line = stringBuilder.BS_Data_from_sensors(valuesAcceleration);
                    //mDbHelper_Sensors.Recording_Data_From_Sensors("ACCELERATION", timenow, finished_line);
                    if (timenow - Test_lasttime > 1000){
                        Test_lasttime = timenow;
                        Log.d(LOG_TAG, "Датчик работает.");
                    }
                    Test_Array.add(finished_line);
                    break;
            }
        }
    };

    private void Data_Processing() {

        Schedule_Data_Processing = Executors.newScheduledThreadPool(1);
        Schedule_Data_Processing.scheduleAtFixedRate(new Runnable() {
            public void run() {
                acquireWakeLock();

                ArrayList<String> Buffer = new ArrayList<>(Test_Array);
                Test_Array.clear();
                //Log.d(LOG_TAG, "Проверка расписания " + Buffer.size());
                Processing_of_data_from_sensors(Buffer);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }


    private void Processing_of_data_from_sensors(ArrayList<String> buffer) {

        long timenow = TakeTimeNow();
        ArrayList<String> Query_Result;
        String row_fot_writing;
        int Interval_Step = 1000; //в миллисекундах. За какой промежуток будем делать выборку
        String sensor_type = "ACCELERATION";

        row_fot_writing = sensors.Acceleration(buffer);
        mDbHelper_Graphs.Recording_Data_for_graphs(sensor_type, timenow, row_fot_writing);
        //Log.d(LOG_TAG, "Запись окончена");
        /*Query_Result = mDbHelper_Sensors.Read_DBData_From_Sensors(sensor_type, timenow, Interval_Step);
        if (Query_Result.size() > 0 && !Query_Result.get(0).isEmpty()){
            row_fot_writing = sensors.Acceleration(Query_Result);
            mDbHelper_Graphs.Recording_Data_for_graphs(sensor_type, timenow, row_fot_writing);
            Log.d(LOG_TAG, sensor_type + row_fot_writing);
        }

        sensor_type = "GYROSCOPE";
        Query_Result = mDbHelper_Sensors.Read_DBData_From_Sensors(sensor_type, timenow, Interval_Step);
        if (Query_Result.size() > 0 && !Query_Result.get(0).isEmpty()){
            row_fot_writing = sensors.Gyroscope(Query_Result);
            mDbHelper_Graphs.Recording_Data_for_graphs(sensor_type, timenow, row_fot_writing);
            Log.d(LOG_TAG, sensor_type + row_fot_writing);
        }*/
    }


    private void Cleaning_of_databases(){

        Schedule_Cleaning_of_databases = Executors.newScheduledThreadPool(1);
        Schedule_Cleaning_of_databases.scheduleAtFixedRate(new Runnable() {
            public void run() {
                Cleaner_DB();
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    private void Cleaner_DB(){
        long timenow = TakeTimeNow();
        int Interval_Step_for_sensors = 120000; //в миллисекундах. За какой промежуток будем делать выборку
        int Interval_Step_for_graphs = 360000; //в миллисекундах. За какой промежуток будем делать выборку
        mDbHelper_Sensors.Clear_DB_for_sensors(timenow, Interval_Step_for_sensors);
        mDbHelper_Graphs.Clear_DB_for_graphs(timenow, Interval_Step_for_graphs);
        Log.d(LOG_TAG, "I cleand the base");
    }

    private long TakeTimeNow(){
        Calendar cal = Calendar.getInstance();
        return cal.getTimeInMillis();
    }

    //Send custom notification
    public void sendNotification(String Ticker,String Title,String Text) {

        //These three lines makes Notification to open main activity after clicking on it
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentIntent(contentIntent)
                .setOngoing(true)   //Can't be swiped out
                .setSmallIcon(R.mipmap.ic_launcher)
                //.setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.large))   // большая картинка
                .setTicker(Ticker)
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
            mWakeLock.acquire();
        }

    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
