package com.projects.firstapptutorial;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.projects.firstapptutorial.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Stream;

import com.projects.firstapptutorial.Utils;

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
    final int CACHE_TIMESTAMPS = 300;
    boolean timeToAnalyse = false;
    float[][] accelArray = new float[CACHE_TIMESTAMPS][3];

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
        textView.setText(accelArray[0][0]+"\n"+accelArray[0][1]+"\n"+accelArray[0][2]);
        for (int j=0; j<3; j++) {
            accelArray[currTimestamp][j] = event.values[j];
        }
        currTimestamp++;
        if (currTimestamp == CACHE_TIMESTAMPS) {
            timeToAnalyse = true;
            currTimestamp = 0;
        }

        if (timeToAnalyse) {
            try {
                // compute magnitudes from cache to find a window on which to center
                float[] magnArray = new float[CACHE_TIMESTAMPS];
                for (int i = 0; i < CACHE_TIMESTAMPS; i++) {
                    magnArray[i] = (float) (Math.pow(accelArray[i][0],2) + (Math.pow(accelArray[i][1],2)) + (Math.pow(accelArray[i][2],2)));
                }
                int indexMagnMax = Utils.argMax(magnArray, HALF_WINDOW, CACHE_TIMESTAMPS-HALF_WINDOW-1);
                textDetect.setText(String.valueOf(indexMagnMax));

                // fill a flatten array with the data for analysis
                float[] flattenArray = new float[ANALYSIS_TIMESTAMPS * 3];

//                for (int i = indexMagnMax - HALF_WINDOW; i < indexMagnMax + HALF_WINDOW + 1; i++) {
//                    for (int j = 0; j < 3; j++) {
//                        flattenArray[j * ANALYSIS_TIMESTAMPS + (i - indexMagnMax - HALF_WINDOW)] = accelArray[i][j];
//                    }
//                }

                int[] shape = new int[]{1, 151, 3};

                Model model = Model.newInstance(getApplicationContext());
                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(shape, DataType.FLOAT32);
                inputFeature0.loadArray(flattenArray, shape);

                // Runs model inference and gets result.
                Model.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                // Releases model resources if no longer used.
                model.close();

//                textDetect.setText(outputFeature0.getFloatArray()[0] + "\n" + outputFeature0.getFloatArray()[1]);
            } catch (IOException e) {
                // TODO Handle the exception
            }
            timeToAnalyse = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}