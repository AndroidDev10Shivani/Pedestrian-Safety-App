package software.user.example.admin.pedestrian_safety;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import software.config.DeviceOrientation;

import static software.user.example.admin.pedestrian_safety.ScreenService.ScreenReceiver.wasScreenOn;

public class ScreenService extends Service  implements SensorEventListener ,LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,TextToSpeech.OnInitListener {

    BroadcastReceiver mReceiver=null;

    //TODO For Steps Count Detection
    private SensorManager mSensorManager;
    private Sensor mStepDetectorSensor;
//    private StepsDBHelper mStepsDBHelper;

    //TODO For Device Orientation
    Sensor accelerometer;
    Sensor magnetometer;
    DeviceOrientation deviceOrientation;
    int orentation_values  = 0;

    int delay = 5000; //milliseconds
    Handler handleers;

//    private ArrayList<DateStepsModel> mStepCountList;

    //TODO For Location
    //Location
    LocationManager locationManager;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    boolean isLocationChanged = false;
    private static int UPDATE_INTERVAL1 = 3000; // 30 sec
    private static int FATEST_INTERVAL = 1500; // 15 sec
    private static int DISPLACEMENT = 10; // 10 meters

    double lat_values,long_values;
    LocationManager mLocationManager;

    //TODO MQTT Connection
    public static final String URL = "tcp://iot.eclipse.org:1883";
    MqttAndroidClient client;
    String clientId;
    boolean isStep_countChanged = false,isDistanceChamged = false;
    int old_distance = 0,orld_orientation = 0;
    private static final int OBSTACLE_DISTANCE = 150;
    TextToSpeech mTts;
    int ring_count = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Register receiver that handles screen on and screen off logic
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);

        //TODO Text to Speech
        mTts = new TextToSpeech(this,
                this  // OnInitListener
        );

        mTts.setSpeechRate(0.5f);

        //TODO MQTT Connection
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(getApplicationContext(),URL,clientId);

        Log.e("####"," Service is started and config with MQTT service");
        configMQTT();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (statusOfGPS) {

//            Log.d("####", "checking play services status is as "+checkPlayServices());

            if (checkPlayServices()) {
//                Log.d("####", "checking play services status is as "+checkPlayServices());

                buildGoogleApiClient();
                createLocationRequest();

            }

            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();

//                Log.e("####", "client connect called");
            }

            Toast.makeText(getApplicationContext(), "Location service started", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this, "Please put on the GPS.", Toast.LENGTH_SHORT).show();
        }

//      TODO FOr Sensor Step Detector
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        Log.e("#####"," Sensor Values is as follow as "+mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR));
        if (mSensorManager == null){
//            Log.e("####"," Now we are making text to Speech ");
//            mTts.speak("Your Device does not support Step Detection Sensor. ",TextToSpeech.QUEUE_FLUSH,null);
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null) {
            mStepDetectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
            mSensorManager.registerListener(this, mStepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
//            mStepsDBHelper = new StepsDBHelper(this);
        }

        handleers = new Handler();
//        mStepCountList = new ArrayList<>();

        //TODO To get Device orientation
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        deviceOrientation = new DeviceOrientation();

        handleers.postDelayed(new Runnable(){
            public void run(){

                orentation_values = deviceOrientation.getOrientation();

                switch (orentation_values){

                    case 1:
                        Log.e("###", " LandScape Orientation");
                       // Toast.makeText(getApplicationContext(),"LandScape Orientation", Toast.LENGTH_SHORT).show();
                        break;

                    case 3:
                        Log.e("###", " LandScape Reverse Orientation");
                       // Toast.makeText(getApplicationContext(),"LandScape Reverse Orientation", Toast.LENGTH_SHORT).show();
                        break;

                    case 6:
                        Log.e("###", " Portrait Orientation");
                       // Toast.makeText(getApplicationContext(),"Portrait Orientation", Toast.LENGTH_SHORT).show();
                        break;

                    case 8:
                        Log.e("###", " Portrait reverse Orientation");
                       // Toast.makeText(getApplicationContext(),"Portrait reverse Orientation", Toast.LENGTH_SHORT).show();
                        break;
                }

                //do something
                handleers.postDelayed(this, delay);

            }
        }, delay);

        //Location Service
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    boolean isOrentationChanged = false;
    Ringtone r;

    private void calculaetStepCounts(){

        Log.e("####"," Now Device is Screen status as for calculation is "+wasScreenOn);

        if (wasScreenOn) {

//            Log.e("####"," List size is as follow "+mStepCountList.size());
            orentation_values = deviceOrientation.getOrientation();

//            Log.e("####"," Orientation values in service "+orentation_values+" and Old Orientation as "+orld_orientation);

            if (orld_orientation != orentation_values){
                orld_orientation = orentation_values;
                isOrentationChanged = true;
            }else {
                isOrentationChanged = false;
            }

            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            Log.e("####","Alarm Conditions "+isOrentationChanged+"  Distance: "+isDistanceChamged+" Steps Counts: "+isStep_countChanged);

            if ((isStep_countChanged || isOrentationChanged) && isStep_countChanged){

                if (isDistanceChamged){

                    ring_count++;

//                    try {
                        if (ring_count == 3) {

                            r.stop();
                            ring_count = 0;

                            //TODO Resetting all the flag to false.
                            isDistanceChamged = false;
                            isOrentationChanged = false;
                            isStep_countChanged = false;

                        }else {
                            r.play();
                        }

                }

            } else if (!isStep_countChanged){
                r.stop();
            }else if (isOrentationChanged && !isStep_countChanged){
                r.stop();
            }

        }else {

            Log.e("####"," Now user screen is off "+wasScreenOn);

            //TODO Disconnecting MQTT
            dissconnectMQTT();

        }
    }


    @Override
    public void onStart(Intent intent, int startId) {

        boolean screenOn = false;

        try{
            screenOn = intent.getBooleanExtra("screen_state", false);

        }catch(Exception e){}

        if (!screenOn) {

            Toast.makeText(getBaseContext(), "Screen on, ", Toast.LENGTH_SHORT).show();

        } else {

            Toast.makeText(getBaseContext(), "Screen off,", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //TODO Screen Orientation
        mSensorManager.registerListener(deviceOrientation.getEventListener(), accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(deviceOrientation.getEventListener(), magnetometer, SensorManager.SENSOR_DELAY_UI);

        return Service.START_STICKY;
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
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {

                    Log.d("#####","The subscription could not be performed");

                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    String dist;
    private void configMQTT(){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);

        try {

            IMqttToken token = client.connect(options);
            Log.e("###"," Now we are connecting under try block "+token);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    Log.d("#####","Connection success");

                    Toast.makeText(getApplicationContext(),"Connected Success!", Toast.LENGTH_SHORT).show();

                    getSharedPreferences(getPackageName(),MODE_PRIVATE).edit()
                            .putString("RecentUrl",URL).apply();

                    subscribeTopics("Udistance");

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    Toast.makeText(getApplicationContext(),"Connection failed ", Toast.LENGTH_SHORT).show();
                    Log.e("#####","Connection failed "+exception.getMessage());
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(),"Exception connecting "+e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("####"," Catch block not connecting "+e.getMessage());
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("********** Connection lost ***********");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                Log.d("#####","Message arrived");

                if(topic.equals("Udistance")){

                    Log.d("#####","distance"+new String(message.getPayload()));

                    dist = new String(message.getPayload());

                        Log.e("#####"," OLD Distance "+old_distance+" new Distance "+dist);
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

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void onDestroy() {

        Log.i("ScreenOnOff", "Service  distroy");
        if (mReceiver != null)
            unregisterReceiver(mReceiver);

        try {
            Toast.makeText(getApplicationContext(), "destoy service", Toast.LENGTH_LONG).show();
            stopLocationUpdates();

        } catch (Exception e) {
            Log.d("###", "exception stopping service " + e.toString());
        }

        dissconnectMQTT();

    }

    private void dissconnectMQTT(){

        //TODO MQTT disconnecting
        try {
            if(client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }

        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

//        mStepsDBHelper.createStepsEntry();
        Toast.makeText(this, "Now user is walking.", Toast.LENGTH_SHORT).show();
        Log.e("####","Now user is walking in service ");
        isStep_countChanged = true;
        calculaetStepCounts();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

//        Log.e("####", "building client for location updates");

    }


    /**
     * Method to verify google play services on the device
     */
    private boolean checkPlayServices() {

        GoogleApiAvailability api = GoogleApiAvailability.getInstance();

        int resultCode = api.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (api.isUserResolvableError(resultCode)) {

            } else {
                Toast.makeText(getApplicationContext(), "This device is not supported.", Toast.LENGTH_LONG).show();

            }
            return false;
        }
        return true;
    }

    /**
     * Creating location request object
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL1);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters

        Log.e("####", " Creating location requestCreating location request");

    }

    /**
     * Starting the location updates
     */
    protected void startLocationUpdates() {

        Log.e("####", " Starting location updates");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    /**
     * Stopping location updates
     */
    protected void stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }


    }

    @Override
    public void onConnected(Bundle bundle) {

        startLocationUpdates();
        System.out.print("#### google api client connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        System.out.print("####    connection failed");
    }

    String location_name;

    @Override
    public void onLocationChanged(Location location) {

        isLocationChanged = true;
        lat_values = location.getLatitude();
        long_values = location.getLongitude();
        Log.e("####", "location changed " + location.getLatitude() + "," + location.getLongitude()+" User Speed is as "+location.getSpeed());

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;

        try {

            addresses = geocoder.getFromLocation(lat_values, long_values, 1);
            String cityName = addresses.get(0).getAddressLine(0);
            String stateName = addresses.get(0).getAddressLine(1);
            String countryName = addresses.get(0).getAddressLine(2);
            Log.e("####", " City name is as follow as " + cityName);
            location_name = cityName + " " + stateName + " " + countryName;

        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e("####", "location changed " + location.getLatitude() + "," + location.getProvider() + " Name " + location_name);

    }

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("####", "Service Language is not available.");
            } else {

            }
        } else {
//            Log.e("#####", "Service Could not initialize TextToSpeech.");
        }

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


}
