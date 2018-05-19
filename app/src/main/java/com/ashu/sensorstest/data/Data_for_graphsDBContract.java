package com.ashu.sensorstest.data;

import android.provider.BaseColumns;

class Data_for_graphsDBContract {
    private Data_for_graphsDBContract(){

    }

    public static final class   DBDataCollection implements BaseColumns{
        public final static String TABLE_NAME = "data_for_graphs";

        public final static String Column_SensorType = "SensorType";
        public final static String Column_DataTime = "DataTime";
        public final static String Column_SensorData = "SensorData";
    }

}
