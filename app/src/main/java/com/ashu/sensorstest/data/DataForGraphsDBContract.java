package com.ashu.sensorstest.data;

import android.provider.BaseColumns;

class DataForGraphsDBContract {
    private DataForGraphsDBContract(){

    }

    public static final class   DBDataCollection implements BaseColumns{
        public final static String stTableName = "data_for_graphs";

        public final static String stColumnSensorType = "SensorType";
        public final static String stColumnDataTime = "DataTime";
        public final static String stColumnSensorData = "SensorData";
    }

}
