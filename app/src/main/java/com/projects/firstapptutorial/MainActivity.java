package com.projects.firstapptutorial;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.projects.firstapptutorial.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.Arrays;

import com.github.psambit9791.jdsp.filter.Butterworth;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView textView;
    private Button start;
    private TextView textDetect;
    private Bitmap bitmap;
    private SensorManager sensorManager;
//    private Sensor sensor;

    int currTimestamp = 0;
    final int ANALYSIS_TIMESTAMPS = 151;
    final int HALF_WINDOW = 75;
    final int CACHE_TIMESTAMPS = 8*HALF_WINDOW;
    final int FILTER_BUFFER_SIZE = 4*HALF_WINDOW;

    boolean timeToAnalyse = false;
    float[][] cacheAccel = new float[CACHE_TIMESTAMPS][3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text_accelerometer);
        start = (Button) findViewById(R.id.button);
        textDetect = (TextView) findViewById(R.id.textView2);

//        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        sensorManager.registerListener( MainActivity.this, sensor, sensorManager.SENSOR_DELAY_NORMAL);




        start.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                    // TODO Auto-generated method stub
                    sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                    sensorManager.registerListener(MainActivity.this,
                            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                            20000);
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //here the event.values will provide the data
        //index 0 for x axis, 1 for y axis, 2 for z axis
        textView.setText(cacheAccel[0][0]+"\n"+ cacheAccel[0][1]+"\n"+ cacheAccel[0][2]);
        for (int j=0; j<3; j++) {
            cacheAccel[currTimestamp][j] = event.values[j];
        }
        currTimestamp++;
        if (currTimestamp == CACHE_TIMESTAMPS) {
            timeToAnalyse = true;
            currTimestamp = 7*HALF_WINDOW; // leave one half-window size to refill
        }

        if (timeToAnalyse) {
            try {
                // fill a flatten array with the data for analysis
                float[] flattenArray = new float[CACHE_TIMESTAMPS * 3];

                for (int i = 0; i < CACHE_TIMESTAMPS; i++) {
                    for (int j = 0; j < 3; j++) {
                        flattenArray[j * CACHE_TIMESTAMPS + i] = cacheAccel[i][j];
                    }
                }

                // DEBUG
                Log.d("flattenArray", Arrays.toString(flattenArray));

                //TODO: filter signal before and on more timestamps because filter doesn't work for the first timestamps

                // filter signal in the flatten array (3 times, one for each axis)
                double[] flattenArrayX = Utils.toDoubleArray(Arrays.copyOfRange(flattenArray,
                        0, CACHE_TIMESTAMPS));
                double[] flattenArrayY = Utils.toDoubleArray(Arrays.copyOfRange(flattenArray,
                        CACHE_TIMESTAMPS, 2*CACHE_TIMESTAMPS));
                double[] flattenArrayZ = Utils.toDoubleArray(Arrays.copyOfRange(flattenArray,
                        2*CACHE_TIMESTAMPS, 3*CACHE_TIMESTAMPS));

                int Fs = 50; //Sampling Frequency in Hz
                int order = 3; //order of the filter
                double cutOff = 0.3; //Cut-off Frequency
                Butterworth fltX = new Butterworth(flattenArrayX, Fs); //signal is of type double[]
                double[] resultX = fltX.lowPassFilter(order, cutOff); //get the result after filtering
                Butterworth fltY = new Butterworth(flattenArrayY, Fs); //signal is of type double[]
                double[] resultY = fltY.lowPassFilter(order, cutOff); //get the result after filtering
                Butterworth fltZ = new Butterworth(flattenArrayZ, Fs); //signal is of type double[]
                double[] resultZ = fltZ.lowPassFilter(order, cutOff); //get the result after filtering

                flattenArrayX = Utils.subtractArray(flattenArrayX, resultX);
                flattenArrayY = Utils.subtractArray(flattenArrayY, resultY);
                flattenArrayZ = Utils.subtractArray(flattenArrayZ, resultZ);

                double[] filteredFlattenArray;
                filteredFlattenArray = Utils.concatenate(flattenArrayX, flattenArrayY);
                filteredFlattenArray = Utils.concatenate(filteredFlattenArray, flattenArrayZ);
                flattenArray = Utils.toFloatArray(filteredFlattenArray);

                //DEBUG
                Log.d("Acceleration", Arrays.toString(flattenArrayY));

                // compute magnitudes from cache to find a window on which to center
                float[] magnArray = new float[CACHE_TIMESTAMPS];
                for (int i = 0; i < CACHE_TIMESTAMPS; i++) {
                    magnArray[i] = (float) (Math.pow(flattenArray[i],2) + (Math.pow(flattenArray[CACHE_TIMESTAMPS+i],2)) + (Math.pow(flattenArray[2*CACHE_TIMESTAMPS+i],2)));
                }
                int indexMagnMax = Utils.argMax(magnArray, FILTER_BUFFER_SIZE+HALF_WINDOW, CACHE_TIMESTAMPS-HALF_WINDOW-1);


                // Find index of maximum TODO:change
                float[] flattenArrayToAnalyse = new float[ANALYSIS_TIMESTAMPS*3];
                for (int i = indexMagnMax - HALF_WINDOW; i < indexMagnMax + HALF_WINDOW + 1; i++) {
                    for (int j = 0; j < 3; j++) {
                        flattenArrayToAnalyse[j * ANALYSIS_TIMESTAMPS + (i - (indexMagnMax - HALF_WINDOW))]
                                = flattenArray[j * CACHE_TIMESTAMPS + i];
                    }
                }

                // Pass to model for inference
                int[] shape = new int[]{1, 151, 3};
                Model model = Model.newInstance(getApplicationContext());
                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);
                inputFeature0.loadArray(flattenArrayToAnalyse, shape);

                // Runs model inference and gets result.
                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                // Releases model resources if no longer used.
                model.close();

                textDetect.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1]);
            } catch (IOException e) {
                // TODO Handle the exception
            }
            timeToAnalyse = false;
            cacheAccel = Utils.shiftLeftArray(cacheAccel, HALF_WINDOW);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}