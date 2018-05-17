package com.ashu.sensorstest.data.Data_from_sensors;

import android.provider.BaseColumns;


class Data_from_sensorsDBContract {
    private Data_from_sensorsDBContract(){

    }

    public static final class   DBDataCollection implements BaseColumns{

        public final static String TABLE_NAME = "data_from_sensors";

        public final static String Column_SensorType = "SensorType";
        public final static String Column_DataTime = "DataTime";
        public final static String Column_SensorData = "SensorData";
    }
}
