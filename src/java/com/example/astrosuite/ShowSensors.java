package com.example.astrosuite;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


public class ShowSensors extends AppCompatActivity implements SensorEventListener {

    //Static Final Variables

    private static long SENSOR_DELAY = 500;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static long DEQUE_CLEAR = 5000 + 1000;
    private static final double MAGNETIC_DECLINATION = 8.54598;
    private static final String MAIN_TAG = "MainActivity";

    private static final boolean VERBOSE_FLAG = false;


    private TextView textView;
    private TextView rpyTextView;
    private TextView dahTextView;
    private TextView headingTextView;

    private Spinner spinner;
    private List<String> spinnerList;
    private HashMap<String, Double[]> raDecHash;


    private Handler runCompute;
    private Handler clearDeque;


    private Location loc;

    private int MY_PERMISSIONS_REQUEST_LOCATION;


    //Sensor Based Fields
    private SensorManager sensorManager;


    private List<Sensor> listOfAccelerationSensors;
    private List<Sensor> listOfGravitySensors;
    private List<Sensor> listOfMagnetometers;


    //Sensor Based Variables
    private float[] globalAccelerationValues;
    private float[] globalMagnetValues;


    private LinkedList<Double> azimuthDeque;
    private LinkedList<Double> altitudeDeque;



    private double globalAzimuth;
    private double globalAltitude;
    private int useX;
    private int useY;


    private Long timeDiff;
    private Boolean sensorPrint = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //On create stuff
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_sensors);

        //Gets elements
        textView = findViewById(R.id.Names);
        rpyTextView = findViewById(R.id.RPYText);
        dahTextView = findViewById(R.id.DAHText);
        headingTextView = findViewById(R.id.headingText);
        Button cameraButton = findViewById(R.id.CameraButton);
        Button clearButton = findViewById(R.id.clearDeques);

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch toggle = findViewById(R.id.ToggleSensor);
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch precisionMode = findViewById(R.id.precisionMode);

        spinner = findViewById(R.id.spinner);
        spinnerList = new LinkedList<>();
        raDecHash = new HashMap<>();


        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        timeDiff = System.currentTimeMillis();

        // Creates sensor lists and initial values.
        azimuthDeque = new LinkedList<>();
        altitudeDeque = new LinkedList<>();

        globalAzimuth = 0.0;
        globalAltitude = 0.0;
        globalAccelerationValues = new float[3];
        globalMagnetValues = new float[3];


        //Gets all the sensor of type
        listOfAccelerationSensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
        listOfGravitySensors = sensorManager.getSensorList(Sensor.TYPE_GRAVITY);
        listOfMagnetometers = sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);


        //Gets last known location.
        //Location Fields
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        }
        loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);


        //Star and Planet Calculations
        if (loc == null) {
            Log.e(MAIN_TAG, "Somehow our location was null.");
        }

        Log.i(MAIN_TAG, "Lat:" + loc.getLatitude() + " Long: " + loc.getLongitude());

        try {
            File httpCacheDir = new File(this.getCacheDir(), "https");
            long httpCacheSize = 5 * 1024 * 1024; // 10 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize);
        } catch (IOException e) {
            Log.i("AAH:", "HTTP response cache installation failed:" + e);
        }



        Runnable runnable = () -> {
            try {
                getRaDec();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
        Thread thread = new Thread(runnable);
        thread.start();


        raDecToAzAlt(135, 17, loc.getLatitude(), loc.getLongitude());

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Double[] raDec = raDecHash.get(spinnerList.get(position));
                if (raDec == null || raDec[0] == -99) {
                    rpyTextView.setText(R.string.NotSearching);
                } else if (raDec[1] < 0) {
                    rpyTextView.setText(getString(R.string.notVisible ) + "Az: " + raDec[0] + "\nAlt: " + raDec[1]);
                } else {
                    rpyTextView.setText("Az: " + raDec[0] + "\nAlt: " + raDec[1]);

                }


            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        //Handles the camera event (will be changed with a live view instead potentially.)
        cameraButton.setOnClickListener(v -> dispatchTakePictureIntent());

        clearButton.setOnClickListener(v -> {
            clearDequeues();
            resetCallBacks();

        });


        //Sets the onChange for the toggle switch.
        toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sensorPrint = isChecked;

            if (isChecked) {
                onResume();
            } else {
                onPause();
                setTextViewsOffState();
            }
        });

        precisionMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                SENSOR_DELAY = 5000;
                DEQUE_CLEAR = 60000 + 1000;

            } else {
                SENSOR_DELAY = 500;
                DEQUE_CLEAR = 5000 + 1000;


            }
            clearDequeues();
            resetCallBacks();
        });

        //Creates the auto run handler for updating the text.
        runCompute = new Handler();
        runCompute.post(computationRoutine);
        setTextViewsOffState();

        clearDeque = new Handler();
        clearDeque.post(clearingRoutine);


    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }

    /**
     * Star and planetary calculations
     */



    private Double[] raDecToAzAlt(double ra, double dec, double lat, double lng) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        LocalDateTime epoch = LocalDateTime.of(2000, 1, 1, 0, 0, 0, 0);
        epoch.atOffset(ZoneOffset.UTC);

        double days = (double) ChronoUnit.HOURS.between(epoch, now) / 24;

        double universalTime = (double) now.getHour() + (double) now.getMinute() / 60 + (double) now.getSecond() / (60 * 60);

        double localSTime = (100.46 + 0.985647 * days + lng + 15 * universalTime) % 360;

        Log.v(MAIN_TAG,"Days: " + days + "\nLocalSTime: " + localSTime / 15 + "\nUniversal Time:" + universalTime);

        double hourAngle = localSTime - ra;

        double temp = Math.sin(Math.toRadians(dec)) * Math.sin(Math.toRadians(lat)) + Math.cos(Math.toRadians(dec)) * Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(hourAngle));
        double alt = Math.toDegrees(Math.asin(temp));

        double temp2 = (Math.sin(Math.toRadians(dec)) - Math.sin(Math.toRadians(alt)) * Math.sin(Math.toRadians(lat))) / (Math.cos(Math.toRadians(alt)) * Math.cos(Math.toRadians(lat)));
        double az;

        if (Math.sin(hourAngle) < 0) {
            az = Math.toDegrees(Math.acos(temp2));
        } else {
            az = Math.toDegrees(Math.PI * 2 - Math.acos(temp2));
        }


        Log.v(MAIN_TAG,"Alt: " + alt + "\nAz: " + az);

        return new Double[]{az, alt};


    }

    private void getRaDec() throws IOException {
        //http://archive.eso.org/programmatic/#TAP

        URL url = new URL("https://simbad.u-strasbg.fr/simbad/sim-tap//sync?REQUEST=doQuery&FORMAT=TEXT&LANG=ADQL&MAXREC=200&QUERY=++++++++SELECT%0D%0A++++++++++++++++id%2C%0D%0A++++++++++++++++RA%2C%0D%0A++++++++++++++++DEC%2C%0D%0A++++++++++++++++update_date%0D%0A%0D%0A++++++++FROM+basic+JOIN+ident+ON+oidref+%3D+oid%0D%0A%0D%0A++++++++WHERE+id+in+('polaris'%2C+'orion'%2C+'sirius'%2C+'andromeda'%2C+'deneb'%2C+'altair'%2C'betelgeuse'%2C'Rigel'%2C+'Vega'%2C'Pleiades'%2C+'Antares'%2C'Canopus'%2C'NGC+5139'%2C+'NGC+4755'%2C+'NGC+3372'%2C+'Aldebaran'%2C+'M45'+)");
        HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();

        Scanner s = null;


        //Caching (is apparently already something android has!)
        try {
            int maxStale = 60 * 60 * 24; // tolerate 24hr stale
            urlConnection.addRequestProperty("Cache-Control", "max-stale=" + maxStale);
            InputStream cached = urlConnection.getInputStream();
            Log.v(MAIN_TAG,urlConnection.getHeaderFields().toString());
            s = new Scanner(cached);

            this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Using Cached Data!", Toast.LENGTH_SHORT).show());



        } catch (FileNotFoundException e) {
            Log.v(MAIN_TAG,urlConnection.getHeaderFields().toString());
            rpyTextView.setText("AHH CACHE ERRORS \nGET ADULT!");

            this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "AHHH it wasn't cached!", Toast.LENGTH_LONG).show());

        }

        if (s != null) {
            s.nextLine();
            s.nextLine();
            spinnerList.add("None");
            Double[] apple = {-99.0, -99.0};

            raDecHash.put("None", apple);


            while (s.hasNext()) {
                String[] sArray = s.nextLine().split("\\|");

                //Creates a spinner with name.
                sArray[0] = sArray[0].replace("\"", "");
                sArray[0] = sArray[0].replace("NAME", "");
                spinnerList.add(sArray[0]);

                raDecHash.put(sArray[0], raDecToAzAlt(Double.parseDouble(sArray[1]), Double.parseDouble(sArray[2]), loc.getLatitude(), loc.getLongitude()));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item, spinnerList);
            spinner.setAdapter(adapter);

        }


    }


    /**
     * Computations for azimuth and altitude.
     */


    private void computeAccelerationDAH(float[] accelData) {
        long time = System.currentTimeMillis();

        float x = accelData[0];
        float y = accelData[1];
        float z = accelData[2];

        double yaw = Math.atan(y / (Math.sqrt(x * x + z * z))) * 57.3;
        double dah = 90.0 - yaw;
        altitudeDeque.add(dah);
        if (VERBOSE_FLAG) {
            Log.v(MAIN_TAG, "Accel Calculation:" + (System.currentTimeMillis() - time));

        }
    }

    private void fullAccelRoutine() {
        long time = System.currentTimeMillis();
        long size = altitudeDeque.size();

        double temp = 0;

        for (double a : altitudeDeque) {
            temp = temp + a;
        }
        temp = temp / size;
        globalAltitude = temp;
        headingTextView.setText("Altitude: " + temp);

        if (VERBOSE_FLAG) {
            Log.v(MAIN_TAG, "Accel Routine:" + (System.currentTimeMillis() - time));
        }

    }

    /**
     * Computes the azimuth based on the global acceleration values and magnet values
     */
    private void doAzimuthCalc() {
        long time = System.currentTimeMillis();
        float[] rotationVector = new float[16];
        SensorManager.getRotationMatrix(rotationVector, null, globalAccelerationValues, globalMagnetValues);

        float[] changedRotationMatrix = new float[16];
        SensorManager.remapCoordinateSystem(rotationVector, useX, useY, changedRotationMatrix);


        float[] orientedValues = new float[3];
        SensorManager.getOrientation(changedRotationMatrix, orientedValues);

        globalAzimuth = (radianToMagnetDeg(orientedValues[0]));
        azimuthDeque.add(globalAzimuth);
        if (VERBOSE_FLAG) {
            Log.v(MAIN_TAG, "Azimuth Calculation:" + (System.currentTimeMillis() - time));
        }

    }

    private void mapCoordinateSystem() {
        if (globalAltitude > 45) {
            useX = SensorManager.AXIS_MINUS_X;
            useY = SensorManager.AXIS_MINUS_Y;
        } else {
            useX = SensorManager.AXIS_X;
            useY = SensorManager.AXIS_Z;
        }
    }


    private void fullAzimuthRoutine() {
        long time = System.currentTimeMillis();
        long size = azimuthDeque.size();
        double temp = 0;

        for (double a : azimuthDeque) {
            temp = temp + a;
        }
        temp = temp / size;

        dahTextView.setText("Azimuth: " + temp);
        if (VERBOSE_FLAG) {
            Log.v(MAIN_TAG, "Azimuth Routine:" + (System.currentTimeMillis() - time));
        }

    }


    private double radianToMagnetDeg(double radians) {
        if (radians < 0) {
            radians = ((radians + (2 * Math.PI)) % (2 * Math.PI));
        }
        radians = Math.toDegrees(radians);
        radians = (radians + (360 - MAGNETIC_DECLINATION)) % 360;

        return (radians);


    }

    /**
     * Helper methods for activity management
     */

    private void clearDequeues() {
        altitudeDeque.clear();
        azimuthDeque.clear();


    }


    /**
     * The runnable method that sets the textView value according to the delay.
     */
    private final Runnable computationRoutine = new Runnable() {
        @Override
        public void run() {

            if (sensorPrint) {
                mapCoordinateSystem();
                fullAccelRoutine();
                fullAzimuthRoutine();

            }
            runCompute.postDelayed(this, SENSOR_DELAY);

        }
    };

    /**
     * The runnable method that clears our dequeues often.
     */
    private final Runnable clearingRoutine = new Runnable() {
        @Override
        public void run() {
            clearDequeues();
            clearDeque.postDelayed(this, DEQUE_CLEAR);

        }
    };

    /**
     * A helper method to reset and resynchronize our computations and deque clears.
     */
    private void resetCallBacks() {
        clearDeque.removeCallbacks(clearingRoutine);
        runCompute.removeCallbacks(computationRoutine);
        clearDeque.postDelayed(clearingRoutine, DEQUE_CLEAR);
        runCompute.postDelayed(computationRoutine, SENSOR_DELAY);
    }

    /**
     * A method to remove all callbacks without resetting them.
     */
    private void removeCallBacks() {
        clearDeque.removeCallbacks(clearingRoutine);
        runCompute.removeCallbacks(computationRoutine);

    }

    /**
     * A helper method to synchronize the UI if the user stops requesting data.
     */
    private void setTextViewsOffState() {
        textView.setText(R.string.offState);
        dahTextView.setText(R.string.offState);
        headingTextView.setText(R.string.offState);


    }


    /**
     * Handles sensor change events (a new value)
     *
     * @param event The event given by sensor managers.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        /*
         * Only cares about sensor events if it needs to.
         */
        if (!sensorPrint) {
            return;
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            globalAccelerationValues = event.values;
            doAzimuthCalc();
            computeAccelerationDAH(event.values);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            globalMagnetValues = event.values;
            doAzimuthCalc();
        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            computeAccelerationDAH(event.values);
        }


        if (!(System.currentTimeMillis() - timeDiff < 1000)) {
            doAzimuthCalc();
            textView.setText("" + globalAzimuth);
            timeDiff = System.currentTimeMillis();
        }


    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    /**
     * Handles what happens when we begin listening.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (listOfAccelerationSensors.size() > 0 && listOfGravitySensors.size() > 0 && listOfMagnetometers.size() > 0) {
            for (Sensor s : listOfAccelerationSensors) {
                sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
            }
            for (Sensor s : listOfGravitySensors) {
                sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
            }
            for (Sensor s : listOfMagnetometers) {
                sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_FASTEST);
            }

        }
        resetCallBacks();

    }


    /**
     * Handles how we pause so as to not overload the system.
     */
    @Override
    protected void onPause() {
        super.onPause();
        clearDequeues();
        sensorManager.unregisterListener(this);
        removeCallBacks();

    }


}