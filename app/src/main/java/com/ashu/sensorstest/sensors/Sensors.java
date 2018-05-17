package com.ashu.sensorstest.sensors;

import com.ashu.sensorstest.string_builders.StringBuilders;

import java.util.ArrayList;

public class Sensors {

    private double[] x_value;
    private double[] y_value;
    private double[] z_value;
    private ArrayList<Double> result_of_work;
    private StringBuilders stringBuilder = new StringBuilders();


    static {
        System.loadLibrary("native-lib");
    }

    public native double CountAVR(double[] x_value);

    public String Acceleration(ArrayList<String> Query_Result){
        x_value = new double[Query_Result.size()];
        y_value = new double[Query_Result.size()];
        z_value = new double[Query_Result.size()];
        result_of_work = new ArrayList<>();
        for (int i = 0; i<Query_Result.size(); i++){
            String[] separated = new String[3];
            String[] shared_request = Query_Result.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = shared_request[j].replace(",", ".");

            }
            x_value[i] = Double.parseDouble(separated[0]);
            y_value[i] = Double.parseDouble(separated[1]);
            z_value[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(CountAVR(x_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(x_value));
        }
        if(Double.isNaN(CountAVR(y_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(y_value));
        }
        if(Double.isNaN(CountAVR(z_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(z_value));
        }

        return stringBuilder.BS_Data_for_graphs(result_of_work);
    }

    public String Gyroscope (ArrayList<String> Query_Result){
        x_value = new double[Query_Result.size()];
        y_value = new double[Query_Result.size()];
        z_value = new double[Query_Result.size()];
        result_of_work = new ArrayList<>();
        for (int i = 0; i<Query_Result.size(); i++){
            String[] separated = new String[3];
            String[] shared_request = Query_Result.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = shared_request[j].replace(",", ".");

            }
            x_value[i] = Double.parseDouble(separated[0]);
            y_value[i] = Double.parseDouble(separated[1]);
            z_value[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(CountAVR(x_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(x_value));
        }
        if(Double.isNaN(CountAVR(y_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(y_value));
        }
        if(Double.isNaN(CountAVR(z_value))){
            result_of_work.add(0.0);
        }else{
            result_of_work.add(CountAVR(z_value));
        }

        return stringBuilder.BS_Data_for_graphs(result_of_work);
    }
}
