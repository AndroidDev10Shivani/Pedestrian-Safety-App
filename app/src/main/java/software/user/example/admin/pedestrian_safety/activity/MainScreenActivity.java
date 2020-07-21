package software.user.example.admin.pedestrian_safety.activity;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Locale;

import software.config.DeviceOrientation;
import software.user.example.admin.pedestrian_safety.R;
import software.user.example.admin.pedestrian_safety.ScreenService;

import static software.user.example.admin.pedestrian_safety.activity.MainScreenActivity.ScreenReceiver.wasScreenOn;

public class MainScreenActivity extends AppCompatActivity implements SensorEventListener {

    TextToSpeech t1;
    String TAG = MainScreenActivity.class.getSimpleName();

    TextView txtArduino;

    AlertDialog.Builder builder;
    AlertDialog alertDialog;

    Context context = MainScreenActivity.this;
    LinearLayout screen;
    TextView screen_orientation;
    int touch_count = 0;
    int orentation_values = 0;
    private DeviceOrientation deviceOrientation;

    //TODO MQTT Connection
    public static final String URL = "tcp://iot.eclipse.org:1883";
    MqttAndroidClient client;
    String clientId;

    boolean isDistanceChamged = false;
    int old_distance = 0,orld_orientation = 0;
    Handler handleers;
    boolean isUserTouch = false;
    boolean isStep_countChanged = false;

    int ring_count = 0;

    private static final int OBSTACLE_DISTANCE = 150;

    //TODO For Steps Count Detection
    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        screen= (LinearLayout) findViewById(R.id.screen);
        setTitle("Pedestrian Safety");

        screen = (LinearLayout) findViewById(R.id.screen);
        txtArduino = (TextView) findViewById(R.id.textView);
        screen_orientation = (TextView) findViewById(R.id.screen_orientation);

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.ENGLISH);
                }
            }
        });

        //TODO MQTT Connection
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(getApplicationContext(),URL,clientId);

        screen.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                touch_count++;
                isUserTouch =  true;

                Log.e("#####"," Clicked on Coordinates X "+event.getX()+" Y "+event.getY()+" Touch count as "+touch_count);
                Toast.makeText(context, "Touched Coordinates: X "+event.getX()+" Y "+event.getY(), Toast.LENGTH_SHORT).show();
                calculaetStepCounts();

                return true;
            }
        });

       //TODO Starting Services
        Intent mStepsIntent = new Intent(getApplicationContext(), ScreenService.class);
        startService(mStepsIntent);

        //TODO FOr Sensor Step Detector
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        Log.e("#####"," Sensor Values is as follow as "+mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));
        if (mSensorManager == null){
            Log.e("####"," Now we are making text to Speech ");
            t1.speak("Your Device does not support Step Detection Sensor. ",TextToSpeech.QUEUE_FLUSH,null);
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        //ToDo Check if GPS is ON
        builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS is currently off, in order to get locations  it needs to be turn on. \n" +
                "Kindly click yes to turn the GPS On.");
        t1.speak("Your GPS is currently off, in order to get locations  it needs to be turn on. Kindly click yes to turn the GPS On.", TextToSpeech.QUEUE_FLUSH, null);

        builder.setIcon(android.R.drawable.ic_menu_mylocation);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                t1.speak("You Clicked on Yes Button.", TextToSpeech.QUEUE_FLUSH, null);
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                t1.speak("You Clicked on No Button.",TextToSpeech.QUEUE_FLUSH,null);
                alertDialog.dismiss();
            }
        });

        //TODO check screen is off or ON
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        Log.e("#####"," Screen Status "+wasScreenOn);

    }

    @Override
    protected void onStop() {
        super.onStop();
        dissconnectMQTT();
    }

    String dist;

    @Override
    protected void onStart() {
        super.onStart();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

        try {

            IMqttToken token = client.connect(options);
//            Log.e("###"," Now we are connecting under try block "+token);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    Log.d("#####","Connection success");

                    txtArduino.setText("Your Device connected to hardware is Success!!");
                    txtArduino.setTextColor(Color.GREEN);

                    Toast.makeText(getApplicationContext(),"Connected Success!", Toast.LENGTH_SHORT).show();

                    getSharedPreferences(getPackageName(),MODE_PRIVATE).edit()
                            .putString("RecentUrl",URL).apply();

                    subscribeTopics("Udistance");

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    Toast.makeText(getApplicationContext(),"Connection failed ", Toast.LENGTH_SHORT).show();
//                    Log.e("#####","Connection failed "+exception.getMessage());
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Exception connecting "+e.getMessage(), Toast.LENGTH_SHORT).show();
//            Log.e("####"," Catch block not connecting "+e.getMessage());
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

                txtArduino.setText("Your Device connection to hardware is Lost");
                txtArduino.setTextColor(Color.RED);

                Toast.makeText(context, "Connection lost", Toast.LENGTH_SHORT).show();
//                System.out.println("********** Connection lost ***********");

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                Log.d("#####","Message arrived");

                if(topic.equals("Udistance")){

                    Log.d("#HardwareDistance","Distance From Hardware: "+new String(message.getPayload()));

                    dist = new String(message.getPayload());

                    if (Integer.parseInt(dist) < OBSTACLE_DISTANCE) {
                        if (old_distance != Integer.parseInt(dist)) {
                            isDistanceChamged = true;
                            old_distance = Integer.parseInt(dist);
                            calculaetStepCounts();
                        } else {
                            isDistanceChamged = false;
                        }
                    }
//                    Log.e("####"," Service Distance is as follow as "+dist);
                }

            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

    }

    public void subscribeTopics(final String topic){

        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

//                    Log.d("#####","The topic is subscribed");

                    Toast.makeText(getApplicationContext(),"Subscribed to "+topic,Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    Log.d("#####","The subscription could not be performed");

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    boolean isOrentationChanged = false;
    Ringtone r;

    private void calculaetStepCounts(){

        Log.e("####"," Now Device is on status as "+ ScreenService.ScreenReceiver.wasScreenOn);

        if (ScreenService.ScreenReceiver.wasScreenOn) {
            deviceOrientation = new DeviceOrientation();
            orentation_values = deviceOrientation.getOrientation();

            if (orld_orientation != orentation_values){
                orld_orientation = orentation_values;
                isOrentationChanged = true;
            }else {
                isOrentationChanged = false;
            }

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            Log.e("####","Alarm Conditions: Orientation "+isOrentationChanged+" Touch: "+isUserTouch+"  Distance: "+isDistanceChamged+" Steps Counts: "+isStep_countChanged);

            handleers = new Handler();

            if ((isUserTouch || isOrentationChanged) && isStep_countChanged){

                if (isDistanceChamged){
                    ring_count++;
                    Log.e("####","Ring Count "+ring_count);

//                    try {
                        if (ring_count == 3) {
//                            new Thread().sleep(300000);

                        r.stop();
                        ring_count = 0;

                        //TODO Resetting all the flag to false.
                        isUserTouch = false;
                        isDistanceChamged = false;
                        isOrentationChanged = false;
                        isStep_countChanged = false;
//                        handleers.removeCallbacks(this);

                    }else {
                        r.play();
                    }

                    /*} catch (InterruptedException e) {
                        e.printStackTrace();
                    }*/

                    /*handleers.postDelayed(new Runnable() {
                        public void run() {
                            try {
                                Log.e("####"," Ring Count values on playing as "+ring_count);


                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //do something
                            handleers.postDelayed(this, 2000);
                        }
                    }, 2000);*/


                }

            } else if (isUserTouch && !isStep_countChanged){
                r.stop();
            }else if (isOrentationChanged && !isStep_countChanged){
                r.stop();
            }
        }else {

            Log.e("####"," Now user screen is off "+ ScreenService.ScreenReceiver.wasScreenOn);

            //TODO Disconnecting MQTT
            dissconnectMQTT();

        }
    }

    private void dissconnectMQTT(){

        //TODO MQTT disconnecting
        try {
            if (client != null) {
                if (client.isConnected()) {
                    client.disconnect();
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }catch (Exception e){

        }

        if (t1 != null) {
            t1.stop();
            t1.shutdown();
        }

    }


    @Override
    public void onResume() {
        super.onResume();

        if (!wasScreenOn) {
            // THIS IS WHEN ONRESUME() IS CALLED DUE TO A SCREEN STATE CHANGE
            System.out.println("SCREEN TURNED ON");
            Log.e("#####"," Now Screen is ON onResume touch count "+touch_count);
        } else {
            // THIS IS WHEN ONRESUME() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
            Log.e(TAG," Screen is off in on Resume");

        }

    }

    @Override
    public void onPause() {
        super.onPause();

//        Log.d("####", "##...In onPause()...");
        if (wasScreenOn) {
            // THIS IS THE CASE WHEN ONPAUSE() IS CALLED BY THE SYSTEM DUE TO A SCREEN STATE CHANGE
//            System.out.println("SCREEN TURNED OFF");
            Log.e("#####"," Screen minimized");
        } else {
            // THIS IS WHEN ONPAUSE() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
        }

        dissconnectMQTT();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        Log.e("####","Now user is walking in service ");
        isStep_countChanged = true;
        calculaetStepCounts();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public static class ScreenReceiver extends BroadcastReceiver {

        public static boolean wasScreenOn = true;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                // DO WHATEVER YOU NEED TO DO HERE
                wasScreenOn = false;
                Log.e("####","Now Screen is on "+wasScreenOn);

            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                // AND DO WHATEVER YOU NEED TO DO HERE
                wasScreenOn = true;
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent i = new Intent(MainScreenActivity.this, Activity_Login.class);
            startActivity(i);
            SharedPreferences pref = getApplicationContext().getSharedPreferences("isLogin", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("isLogin", "0");
            editor.commit();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDestroy() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }

        super.onDestroy();
    }
}