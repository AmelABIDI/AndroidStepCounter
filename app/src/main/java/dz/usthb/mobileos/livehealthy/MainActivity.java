package dz.usthb.mobileos.livehealthy;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
// commence à enregistrer recordings des steps et afficher les resultats to user.

public class MainActivity extends AppCompatActivity {
    boolean bound = false;
    private MyService myservice;
    private TextView TvSteps;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    ///my chart stuff
    private LineGraphSeries<DataPoint> series;
    private static double currentX;
    private ThreadPoolExecutor liveChartExecutor;
    boolean running = false;
    ScheduledExecutorService exec;

    ServiceConnection mServiceCoonection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyService.MyBinder mBinder = ( MyService.MyBinder)service;
            myservice=mBinder.getService();
            bound=true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound=false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /****graphe déclarations ici ****/
        GraphView graph = (GraphView) findViewById(R.id.graph);

        series = new LineGraphSeries<>();
        series.setColor(Color.GREEN);
        graph.addSeries(series);

        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);

        // activate horizontal scrolling
        graph.getViewport().setScrollable(true);

        // activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScalableY(true);

        // activate vertical scrolling
        graph.getViewport().setScrollableY(true);
        // To set a fixed manual viewport use this:
        // set manual X bounds
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(1);
        graph.getViewport().setMaxX(6.5);

        // set manual Y bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(10);

        currentX = 0;

        // Start chart thread
        liveChartExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
        if (liveChartExecutor != null)
            liveChartExecutor.execute(new AccelerationChart(new AccelerationChartHandler()));


        /****fin graphe initialisations ****/
        // Get an instance of the SensorManager

        TvSteps = (TextView) findViewById(R.id.tv_steps);




    }

    @Override
    protected void onStart() {
        super.onStart();
        bound=true;
        Log.d("main","started main ...");
        Intent intent = new Intent(this, MyService.class);
        //startService(intent);
        bindService(intent, mServiceCoonection, Context.BIND_AUTO_CREATE);
        startThread();
        Log.d("main","started thread ...");
    }

    @Override
    protected void onStop() { //ici
        super.onStop();
        if(bound){
        unbindService(mServiceCoonection);
        exec.shutdown();
        bound=false;
        }
    }

    public void startThread() {
        final Handler handler = new Handler();
        Runnable workdtodo = new Runnable() {
            @Override
            public void run() {
                if (bound)
                    Log.d("stepnum", myservice.getStep() + "");
                    TvSteps.setText(TEXT_NUM_STEPS + myservice.getStep());
                    // }
//                handler.post(new Runnable() {
//                    @Override
//                    public void run() {
//
//                    }
//                });
            }
        };
        exec =Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(workdtodo ,0,1,TimeUnit.SECONDS);
    }



        public void step ( long timeNs){

            TvSteps.setText( myservice.getStep());
        }


        private class AccelerationChartHandler extends Handler {
            @Override
            public void handleMessage(Message msg) {
                Double accelerationY = 0.0D;
                if (!msg.getData().getString("ACCELERATION_VALUE").equals(null) && !msg.getData().getString("ACCELERATION_VALUE").equals("null")) {
                    accelerationY = (Double.parseDouble(msg.getData().getString("ACCELERATION_VALUE")));
                }

                series.appendData(new DataPoint(currentX, accelerationY), true, 10);
                currentX = currentX + 1;
            }
        }

        private class AccelerationChart implements Runnable {
            private boolean drawChart = true;
            private Handler handler;

            public AccelerationChart(Handler handler) {
                this.handler = handler;
            }

            @Override
            public void run() {
                while (drawChart) {
                    Double accelerationY;
                    try {
                        Thread.sleep(300); // Speed up the X axis
                        accelerationY = myservice.getAccelerationQueue().poll();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        continue;
                    }
                    if (accelerationY == null)
                        continue;

                    // currentX value will be excced the limit of double type range
                    // To overcome this problem comment of this line
                    // currentX = (System.currentTimeMillis() / 1000) * 8 + 0.6;

                    Message msgObj = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("ACCELERATION_VALUE", String.valueOf(accelerationY));
                    msgObj.setData(b);
                    handler.sendMessage(msgObj);
                }
            }
        }

    }


