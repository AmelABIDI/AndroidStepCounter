package dz.usthb.mobileos.livehealthy;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;

public class MyService extends Service implements StepListener,SensorEventListener {
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;

    private int numSteps=0;
    private final IBinder mBinder = new MyBinder();
    private LinkedBlockingQueue<Double> accelerationQueue = new LinkedBlockingQueue<>(10);



    public MyService() {

    }


    //methodes pour recuperer les données sur le graphe
    private void getAccelerometer(SensorEvent event) {
        float[] values = event.values;
        // Movement
        double x = values[0];
        double y = values[1];
        double z = values[2];

        double accelerationSquareRoot = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        double acceleration = Math.sqrt(accelerationSquareRoot);

        accelerationQueue.offer(acceleration);
    }

    public LinkedBlockingQueue<Double> getAccelerationQueue(){
        return  accelerationQueue;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //utiliser data du sensor
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event);
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;

    }
    public int getStep(){
        return numSteps;
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class MyBinder extends Binder {
        MyService getService() {
            return MyService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("myservice","started service ...");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("myservice","onstartCommand service");
        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);
        numSteps = 0;
        sensorManager.registerListener(MyService.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        return super.onStartCommand(intent, flags, startId);
       // return START_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("myservice","destroyed service");
        sensorManager.unregisterListener(MyService.this);

    }

}
