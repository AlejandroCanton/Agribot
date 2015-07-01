package com.example.utd.tryout1;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Activity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import android.support.v4.app.FragmentActivity;
import android.widget.TextView;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;


import java.util.LinkedList;

import java.util.Random;

/*
Uses the Google Maps API to create a Map Fragment,
Uses the Google Location API to get Latitude and Longitude,
Uses the Device's sensors to create a Compass,
Uses a Socket to establish a TCP connection with the Agribot.
 */
public class MainActivity extends FragmentActivity implements
        SensorEventListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        OnMapReadyCallback, OnMapLongClickListener{

    //COMPASS
    SensorManager sensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    private GeomagneticField geomagneticField;

    private float[] valuesAccelerometer;
    private float[] valuesMagneticField;

    private float[] matrixR;
    private float[] matrixI;
    private float[] matrixValues;

    private float declination;

    TextView readingDirection;
    TextView readingTheta;


    //GPS
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private LocationRequest mLocationRequest;
    private boolean hasLocation = false;
    private boolean hasDestination = false;
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private boolean mRequestingLocationUpdates = true;

    private boolean destinationReached = false;

    TextView mLatitudeText;
    TextView mLongitudeText;

    //RES GPS
    //private double destinationLatitude = 32.9862365;
    //private double destinationLongitude = -96.7510076;
    //LAB GPS
    //double destinationLatitude = 32.986270;
    //double destinationLongitude = -96.751029;
    //Empty GPS
    double destinationLatitude;
    double destinationLongitude;
    //OUTSIDE GPS
    //double destinationLatitude = 32.986813;
    //double destinationLongitude = -96.750949;

    //TCP
    TextView textMessage;
    TextView textResponse;

    private Client mClient;
    private boolean hasObstacle = false;

    private String carAction;

    //COMMANDS FOR TCP
    private static final String NOTHING = "nothing";
    private static final String FORWARD = "forward";
    private static final String LEFT = "turnL";
    private static final String RIGHT = "turnR";
    private static final String REVERSE = "rev";
    private static final String STOP = "stop";
    private static final String START = "start";

    //MAPS
    GoogleMap mGoogleMap;
    MapFragment mapFragment;
    private LinkedList linkedList;
    private int markerCounter = 0;
    private int listHead = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GPS
        createLocationRequest();
        buildGoogleApiClient();

        //COMPASS
        createCompass();

        //TCP
        createTCP();

        //THREAD
        //createThread();

        //MAPS
        createMap();

    }



    //
    //COMPASS METHODS
    //

    private void createCompass()
    {
        /*
            Both the accelerometer and the magnetic field sensors are obtained from the phone to use,
            to get the North heading. Because the magnetic field sensors are used, it will calculate
            the magnetic North (which is different from the true north used for maps) to calculate
            the true North using the magnetic one, it is necessary to calculate the magnetic declination.
            The magnetic declination depends on the location of the user, so the GPS' location will be
            used to calculate the declination, and therefore the true North.
         */

        readingDirection = (TextView) findViewById(R.id.textCompass);
        readingTheta = (TextView) findViewById(R.id.textHeading);

        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        valuesAccelerometer = new float[3];
        valuesMagneticField = new float[3];

        matrixR = new float[9];
        matrixI = new float[9];
        matrixValues = new float[3];
    }

    public void onAccuracyChanged(Sensor arg0, int arg1) {
        // TODO Auto-generated method stub

    }

    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                for(int i =0; i < 3; i++){
                    valuesAccelerometer[i] = event.values[i];
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                for(int i =0; i < 3; i++){
                    valuesMagneticField[i] = event.values[i];
                }
                break;
        }

        boolean success = SensorManager.getRotationMatrix(
                matrixR,
                matrixI,
                valuesAccelerometer,
                valuesMagneticField);

        if(success){

            double wantedDirection;

            float orientation[] = new float[3];
            SensorManager.getOrientation(matrixR, orientation);

            double azimuth = Math.toDegrees(matrixValues[0]);
            azimuth = orientation[0]; // contains azimuth, pitch, roll


            double azimuthInRadians = azimuth;
            double azimuthInDegress = Math.toDegrees(azimuthInRadians);
            if (azimuthInDegress < 0.0f) {
                azimuthInDegress += 360.0f;
            }

            //readingAzimuth.setText(String.valueOf(azimuthInDegress));

            double direction = applyDeclination((long) azimuthInDegress, declination);

            readingDirection.setText(
                    String.valueOf(direction));


            if (hasLocation && hasDestination && destinationReached == false)
            {
                wantedDirection = getTheta(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                        destinationLatitude, destinationLongitude);

                ((TextView) findViewById(R.id.textDestinationLat)).setText("" + destinationLatitude);
                ((TextView) findViewById(R.id.textDestinationLon)).setText("" + destinationLongitude);

                //wantedDirection = getTheta(-3, -30, 0, 0);

                //wantedDirection = Math.round(wantedDirection);
                //wantedDirection = 80;

                readingTheta.setText("" + wantedDirection);
                hasObstacle = mClient.hasObstacle;
                textResponse.setText(mClient.message);

                if(mClient.isConnected)
                {
                    if(hasObstacle)
                    {
                        carAction = NOTHING;
                    }
                    else
                    {
                        carAction = checkDirection( (int) direction, (int) wantedDirection);
                        mClient.Send(carAction);
                    }

                    textMessage.setText(carAction);
                }



            }


        }

    }

    private boolean directionBetweenRange (int leftDirection, int rightDirection, int directionToCompare)
    {
        /*
            If the directionToCompare is between the left and right direction, it returns true.
         */

        if(leftDirection < directionToCompare &&
                directionToCompare < rightDirection)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    private String checkDirection(int direction, int wantedDirection)
    {
        /*
            This method uses the phone compass direction and compares it to the heading that is needed
            to get to the next waypoint (wantedDirection). Depending on the comparison, it will change
            the command for the agribot, to go Left, Right or Forward.
         */
        int anotherDirection;

        //toleranceRange is used to go Forward, even when the compass and wantedDirection are not the same,
        //  if wantedDirection is 50 and toleranceRange equals 10, the forward command will be used when
        // the compass is between 40 and 60
        int toleranceRange = 30;
        boolean isInRange;
        boolean isInForwardRange;
        String accion;

        if (    ( ( wantedDirection - toleranceRange ) < 0 &&
                direction >= ( 360 + wantedDirection - toleranceRange) )

                ||

                ( ( wantedDirection + toleranceRange ) > 360 &&
                        direction <= ( wantedDirection + toleranceRange - 360) )

                ||

                ( direction >= (wantedDirection - toleranceRange) &&
                        direction <= (wantedDirection + toleranceRange) )

                )

        {
            isInForwardRange = true;
        }
        else
        {
            isInForwardRange = false;
        }



        if (isInForwardRange)
        {
            //FORWARD
            accion = FORWARD;
            System.out.println("FORWAAAAAARD");
        }
        else if(   (direction - 180)    >= 1 )
        {
            anotherDirection = direction - 180;

            isInRange = directionBetweenRange(anotherDirection, direction, wantedDirection);
            //System.out.println("Left " + anotherDirection + " Right " + direction + " Want " + wantedDirection + " Bool " + isInRange);
            //System.out.println();

            if (isInRange == true)
            {
                //TURN LEFT
                accion = LEFT;
            }
            else
            {
                //TURN RIGHT
                accion = RIGHT;
            }
        }

        else
        {
            anotherDirection = direction + 180;

            isInRange = directionBetweenRange(direction, anotherDirection, wantedDirection);

            if (isInRange == true)
            {
                //TURN RIGHT
                accion = RIGHT;
            }
            else
            {
                //TURN LEFT
                accion = LEFT;
            }

        }

        return accion;

    }



    //
    //Guang Methods for radius and theta
    //

    private double getR(double currentLatitude, double currentLongitude, double finalLatitude, double finalLongitude)
    {
        //The Radius is calculated, depending on the current location and destination
        currentLatitude = currentLatitude * Math.PI/180;
        currentLongitude = currentLongitude * Math.PI/180;
        finalLatitude = finalLatitude * Math.PI/180;
        finalLongitude = finalLongitude * Math.PI/180;
        double fi = Math.acos(Math.cos(currentLatitude) * Math.cos(finalLatitude) * Math.cos(finalLongitude - currentLongitude) + Math.sin(currentLatitude) * Math.sin(finalLatitude));
        return 6371000*fi;

    }

    private double getTheta(double currentLatitude, double currentLongitude, double finalLatitude, double finalLongitude)
    {
        /*
        The heading angle is calculated, considering the quadrant in which the waypoint is located
        from the phone
         */
        double r = getR(currentLatitude, currentLongitude, finalLatitude, finalLongitude);
        double d = getR(currentLatitude, currentLongitude, finalLatitude, currentLongitude);
        double tmp = Math.acos(d/r)*180/Math.PI;
        double theta = 0;
        //System.out.println(currentLatitude + " " + currentLongitude + " " + finalLatitude + " " + finalLongitude);
        if(currentLatitude>finalLatitude){
            if(currentLongitude>finalLongitude){
                ((TextView) findViewById(R.id.textViewLongitude)).setText("Quad NE");
                theta = 180 + tmp;
            }
            else{
                ((TextView) findViewById(R.id.textViewLongitude)).setText("Quad NW");
                theta = 180 - tmp;
            }
        }
        else {
            if(currentLongitude>finalLongitude){
                ((TextView) findViewById(R.id.textViewLongitude)).setText("Quad SE");
                theta = 360 - tmp;
            }
            else{
                ((TextView) findViewById(R.id.textViewLongitude)).setText("Quad SW");
                theta = tmp;
            }
        }
        //return Math.round( 360 - theta )%360;
        return Math.round(theta)%360;
        //return 360-theta;
    }



    //
    //TCP METHODS
    //


    private void createTCP()
    {
        /*
            Using a Client object with Socket properties, a TCP connection will be established
            with the Agribot, to give commands and receive messages from it.

            One should change the IP address in the Client class if needed.
         */
        textMessage = (TextView) findViewById(R.id.textMessage);
        textResponse = (TextView) findViewById(R.id.textResponse);

        mClient = new Client();
        Thread thread = new Thread(mClient);
        thread.start();
    }

    private void createThread()
    {
        /*
        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                try{
                    Random rand = new Random();
                    int randomNum = rand.nextInt(6);

                    //System.out.println(randomNum);
                    //carAction = randomNum;
                    //setCarAction(carAction);


                    //also call the same runnable
                    handler.postDelayed(this, 10);
                }
                catch (Exception e) {
                    // TODO: handle exception
                }

                finally{
                    //also call the same runnable
                    handler.postDelayed(this, 1000);
                }
            }
        };

        handler.postDelayed(runnable, 1000);
        */
    }



    //
    //GPS METHODS
    //

    protected synchronized void buildGoogleApiClient()
    {
        /*
            The Google Api Client is built with the location services API
         */
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    public void onConnected(Bundle connectionHint) {
        /*
            If the app is able to connect to Google Services, the location is obtained
            using them
         */
        System.out.println("Success");

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            System.out.println("Lat " + mLastLocation.getLatitude());
            System.out.println("Lon " + mLastLocation.getLongitude());


            hasLocation = true;
            createGeomagneticField();

            updateUI();

            if (mRequestingLocationUpdates) {
                startLocationUpdates();
            }

            //If the map is already started, its camera will be moved to the current location
            if (mGoogleMap != null)
            {
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                      new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 18));
            }


        }
        else{
            System.out.println("No change");
        }
    }

    public void onConnectionSuspended(int i) {
    }

    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        //mLatitudeTextView.setText(String.valueOf(mCurrentLocation.getLatitude()));
        //mLongitudeTextView.setText(String.valueOf(mCurrentLocation.getLongitude()));
        //mLastUpdateTimeTextView.setText(mLastUpdateTime);

        ((TextView)findViewById(R.id.textLatitude)).setText("" + mLastLocation.getLatitude());
        ((TextView)findViewById(R.id.textLongitude)).setText("" + mLastLocation.getLongitude());

        if (   isInDestination(mLastLocation.getLatitude(), mLastLocation.getLongitude(),
                destinationLatitude, destinationLongitude)   )
        {
            carAction = STOP;
            mClient.Send(carAction);

            //If it is the last waypoint it ends, if not, it will go to the next marker
            if(markerCounter == (listHead + 1)   )
            {
                destinationReached = true;
                textMessage.setText("You have arrived");
            }
            else
            {
                deleteCurrentMarker();
                goNextMarker();
            }

        }

    }

    private boolean isInDestination(double currentLatitude, double currentLongitude, double finalLatitude, double finalLongitude)
    {
        /*
            By comparing the current location with the final location, it can be determined
            when the agribot has arrived its destination (waypoint or final destination)
         */


        //A tolerance range to determine how close to the final destination it has to be,
        //to be considered as successful... the smaller the number, better accuracy but harder to achieve.
        double range = .0000300;

        if(  currentLatitude >= (finalLatitude - range) &&
                currentLatitude <= (finalLatitude + range) &&
                currentLongitude >= (finalLongitude - range) &&
                currentLongitude <= (finalLongitude + range) )
        {
            return true;
        }

        return false;
    }



    //
    //GEOMAGNETIC FIELD METHODS
    //

    private void createGeomagneticField()
    {
        /*
            Creates a geomagneticField with the current location
         */
        geomagneticField = new GeomagneticField(
                (float) mLastLocation.getLatitude(),
                (float) mLastLocation.getLongitude(),
                (float) mLastLocation.getAltitude(),
                System.currentTimeMillis());

        declination = geomagneticField.getDeclination();
    }

    private long applyDeclination(long heading, float declination) {
        return Math.round( heading+declination+360 )%360;
    }



    //
    //MAPS METHODS
    //

    private void createMap()
    {
        /*
             A map fragment is created with the Google Maps API, as well as a LinkedList to
             keep the markers (Waypoints) in the Map.
         */
        linkedList = new LinkedList();

        mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    public void onMapLongClick(LatLng latLng) {
        //When the user presses the screen for a long time, the method is used
        addMarker(latLng);
    }

    public void onMapReady(GoogleMap map) {

        //When the map is ready for use, it will be configured and reposition itself
        // if the gps already has the location.
        mGoogleMap = map;

        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMapLongClickListener(this);

        if (mLastLocation != null)
        {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 18));
        }

    }

    private void addMarker(LatLng latLng)
    {
        /*
            A marker is added to the map, with the specified coordinates,
            and then added to the linkedlist
         */
        markerCounter++;

        Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(Integer.toString(markerCounter)));

        linkedList.addLast(marker);

    }

    private void deleteCurrentMarker()
    {
        /*
            The current marker head in the list is deleted from the map.
         */
        Marker marker;
        marker =  (Marker) linkedList.get(listHead);

        marker.setVisible(false);
        listHead++;

    }

    private void goNextMarker()
    {
        /*
            It takes the head of the marker list, and assigns its latitude and longitude
            as the new destination
         */
        Marker marker;
        marker = (Marker) linkedList.get(listHead);

        destinationLatitude = marker.getPosition().latitude;
        destinationLongitude = marker.getPosition().longitude;

        //System.out.println(linkedList.getFirst());
        System.out.println(destinationLatitude + " " + destinationLongitude);
    }




    //THE APPLICATION STARTS, ONLY AFTER THE USER HAS SET AT LEAST 1 WAYPOINT
    public void startApp(View view)
    {

        if (linkedList.peekFirst() != null)
        {
            hasDestination = true;
            goNextMarker();
            //goNextMarker();
            destinationReached = false;
            carAction = NOTHING;
        }


    }



    //TCP Methods used with the buttons on the UI
    public void goRight(View view) {
        System.out.println("SEND     turnR");
        mClient.Send("turnR");
    }

    public void goBackwards(View view) {
        System.out.println("SEND     rev");
        mClient.Send("rev");
    }

    public void goForward(View view) {
        System.out.println("SEND     forward");
        mClient.Send("forward");
    }

    public void goLeft(View view) {
        System.out.println("SEND     turnL");
        mClient.Send("turnL");
    }

    public void goStop(View view) {
        System.out.println("SEND     stop");
        destinationReached = true;
        carAction = STOP;
        mClient.Send(carAction);
        textMessage.setText(carAction);
    }

    public void goStart(View view) {
        System.out.println("SEND     Start");
        mClient.Send("start");
    }



    //USED FOR DEBUGGING... SHOULD BE DELETED LATER...
    public void setNewDirection(View view){

        EditText editLatitude = (EditText) findViewById(R.id.editLatitude);
        EditText editLongitude = (EditText) findViewById(R.id.editLongitude);
        String aString;
        double number;

        //Set Latitude
        aString = editLatitude.getText().toString();
        number = Double.parseDouble(aString);
        destinationLatitude = number;
        System.out.println("Destination lat" + destinationLatitude);

        //Set Longitude
        aString = editLongitude.getText().toString();
        number = Double.parseDouble(aString);
        destinationLongitude = number;
        System.out.println("Destination lon" + destinationLongitude);

        hasDestination = true;
    }



    //ON METHODS
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, sensorMagneticField, SensorManager.SENSOR_DELAY_NORMAL);
        if (mGoogleApiClient.isConnected() && !mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    protected void onPause() {

        sensorManager.unregisterListener(this, sensorAccelerometer);
        sensorManager.unregisterListener(this, sensorMagneticField);
        stopLocationUpdates();
        super.onPause();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
