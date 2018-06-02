package com.ashu.sensorstest.sensors;

import com.ashu.sensorstest.string_builders.StringBuilders;

import java.util.ArrayList;


/*
В данном классе обрабатываются данные с сенсоров за минуту. На входе имеем массив, на
выходе среднее значение.
Методы для акселерометра и гироскопа идентичны, т.к. они дают на выходе 3 переменные и
их можно было бы свести к одному и сделать более универсальным. Но решил оставить, дабы показать, что:
1) обработку данных внутри метода для каждого датчика можно настроить как угодно, и это не повлияет
на обработку данных для других датчиков;
2) можно добавить/удалить любое количество других датчиков, и это также не повлияет на работу
остальных.
 */
public class Sensors {

    private double[] dXValue;
    private double[] dYValue;
    private double[] dZValue;
    private ArrayList<Double> arResultOfWork;
    private StringBuilders stringBuilder = new StringBuilders();


    static {
        System.loadLibrary("native-lib");
    }

    public native double countAVR(double[] dXValue);


    /*
        На вход получаем массив строк.
        Из кадой строки получаем XYZ. Их записываем в соответстувющие массивы.
        Полученные массивы отправляем на обработку С++ методом.
        На выходе после каждой обработки имеем 1 одно среднее значение для соотвествующей оси.
        Три полученных значения собираем в строку.
     */
    public String acceleration(ArrayList<String> arrValue){
        dXValue = new double[arrValue.size()];
        dYValue = new double[arrValue.size()];
        dZValue = new double[arrValue.size()];
        arResultOfWork = new ArrayList<>();
        for (int i = 0; i<arrValue.size(); i++){
            String[] separated = new String[3];
            String[] sharedRequest = arrValue.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = sharedRequest[j].replace(",", ".");

            }
            dXValue[i] = Double.parseDouble(separated[0]);
            dYValue[i] = Double.parseDouble(separated[1]);
            dZValue[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(countAVR(dXValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dXValue));
        }
        if(Double.isNaN(countAVR(dYValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dYValue));
        }
        if(Double.isNaN(countAVR(dZValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dZValue));
        }

        return stringBuilder.bsDataForGraphs(arResultOfWork);
    }

    public String gyroscope(ArrayList<String> arrValue){
        dXValue = new double[arrValue.size()];
        dYValue = new double[arrValue.size()];
        dZValue = new double[arrValue.size()];
        arResultOfWork = new ArrayList<>();
        for (int i = 0; i<arrValue.size(); i++){
            String[] separated = new String[3];
            String[] sharedRequest = arrValue.get(i).split(";");
            for (int j = 0; j < 3; j++){

                separated[j] = sharedRequest[j].replace(",", ".");

            }
            dXValue[i] = Double.parseDouble(separated[0]);
            dYValue[i] = Double.parseDouble(separated[1]);
            dZValue[i] = Double.parseDouble(separated[2]);

        }
        if(Double.isNaN(countAVR(dXValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dXValue));
        }
        if(Double.isNaN(countAVR(dYValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dYValue));
        }
        if(Double.isNaN(countAVR(dZValue))){
            arResultOfWork.add(0.0);
        }else{
            arResultOfWork.add(countAVR(dZValue));
        }

        return stringBuilder.bsDataForGraphs(arResultOfWork);
    }
}
