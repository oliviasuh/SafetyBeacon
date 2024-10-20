package com.example.safetyalarmapp;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.widget.Toast;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Context;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import android.media.MediaPlayer;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
import java.util.ArrayList;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import android.content.Intent;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_CODE = 101;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private SensorManager sensorManager;
    private VibrateDetector vibrateDetector;
    private TextView vibrateCountTextView;  // Reference to the TextView
    private MediaPlayer panicSound;
    private Button buttonPanicSound;
    private Button buttonTestVibration;
    private Button buttonCall911;
    private boolean isPlaying = false;
    private EditText contactListInput;
    public double lat,lon;
    public static double latt,lonn;
    private FusedLocationProviderClient client;
    private LocationManager locationManager;
    private double latitude, longitude;
    private String googleMapsLink;
    private String locationText = "Location not found";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the TextView
        vibrateCountTextView = findViewById(R.id.vibrationCountTextView);
        // Initialize SensorManager and VibrateDetector
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Init resources
        buttonPanicSound = findViewById(R.id.buttonSound);
        buttonTestVibration = findViewById(R.id.testShakeButton);
        buttonCall911 = findViewById((R.id.call911Button));
        contactListInput = findViewById((R.id.contactListInput));

        // Initialize the panic sound
        panicSound = MediaPlayer.create(this, R.raw.police_siren);
        isPlaying = false;

        vibrateDetector = new VibrateDetector(vibrateCount -> {
            // Update the TextView with the current shake count
            vibrateCountTextView.setText("Vibrate Count: " + vibrateCount);
            if(vibrateCount == 5) {
                // Triggered when a shake is detected
                sendAlertMessage();
                makeEmergencyCall();
                Toast.makeText(MainActivity.this, "Emergency Detected! Sending alarm msg", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCall911.setOnClickListener( view->requestPermissions());
        // Register the listener
        if (accelerometer != null) {
            sensorManager.registerListener( vibrateDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        // Check for SMS permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
        // Request GPS and SMS permissions at runtime
        requestPermissions();
        // Initialize GPS location
        client= LocationServices.getFusedLocationProviderClient(this);
        client.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location loc) {
                lat=loc.getLatitude();
                lon=loc.getLongitude();
                Log.i("Loca",String.valueOf(+lat));
                Log.i("Loca",String.valueOf(+lon));
                latt=lat;
                lonn=lon;
            }
        });


        buttonPanicSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (panicSound != null) {
                    if(isPlaying){
                        stopSound();
                    }
                    else{
                        playSound();
                    }
                }
            }
            });
        buttonTestVibration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendAlertMessage();
               // getLocationAndSendSMS();
                }
        });
    }
    // Play the sound and update button text
    private void playSound() {
        panicSound.start();
        isPlaying = true;
        buttonPanicSound.setText("Stop Sound");

        // Listener to detect when the sound finishes playing
        panicSound.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopSound();  // Automatically stop and reset button text
            }
        });
    }

    // Stop the sound and update button text
    private void stopSound() {
        if (panicSound.isPlaying()) {
            panicSound.pause();  // Pause the sound
            panicSound.seekTo(0);  // Reset to the beginning
        }
        isPlaying = false;
        buttonPanicSound.setText("Play Sound");
    }
    // Function to send an alert message to emergency contacts
    private void sendAlertMessage() {
        String googleMapsLink = "https://maps.google.com/?q="+locationText;
        String message = "Emergency! Help needed. Here's my location: " + googleMapsLink;

        //contactListInput.setText("9136265044");
        String contacts = contactListInput.getText().toString();
        String[] emergencyContacts = contacts.split(",");
        Log.d("sendAlertMessage", "GoogleMapLink " + googleMapsLink);
        //ArrayList<String> emergencyContacts = new ArrayList<>();
       // emergencyContacts.add("9136265044");  // Replace with actual contact numbers
       // emergencyContacts.add("9134066777");

        // Play the panic sound
        if (panicSound != null) {
            playSound();
           //  panicSound.start();
           // panicSound.setLooping(true);
           // isPlaying = true;
        }

        SmsManager smsManager = SmsManager.getDefault();
        for (String contact : emergencyContacts) {
            try {
                smsManager.sendTextMessage(contact, null, message, null, null);
                Log.d("VibrationAlert", "SMS sent to " + contact);
            } catch (Exception e) {
                Log.e("VibrationAlert", "Failed to send SMS: " + e.getMessage());
            }
        }
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            // Check if GPS is enabled
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        1000, // Minimum time interval for updates (in milliseconds)
                        1,    // Minimum distance interval for updates (in meters)
                        locationListener2
                );
            } else {
                Toast.makeText(this, "Please enable GPS", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }
    private void makeEmergencyCall() {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:911"));

        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "Permission denied or issue making the call",
                    Toast.LENGTH_SHORT).show();
        }
    }
    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.CALL_PHONE
            }, PERMISSION_REQUEST_CODE);
        } else {
            // Permissions are already granted
            getLocation();
            makeEmergencyCall();
        }
    }

    private final LocationListener locationListener2 = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            locationText = latitude + "," + longitude;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(@NonNull String provider) {}

        @Override
        public void onProviderDisabled(@NonNull String provider) {}
    };
    private void getLocationAndSendSMS() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.d("requestLocationUpdates", "latitude=" + latitude + ",longitude="+longitude);
                  //  sendAlertMessage();
                    locationManager.removeUpdates(this);  // Stop location updates to save battery
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
                makeEmergencyCall();
            } else {
                Toast.makeText(this, "SMS/Call permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation();  // Start location retrieval
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister listener to save battery when activity is not active
        sensorManager.unregisterListener(vibrateDetector);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register listener when activity is resumed
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(vibrateDetector, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (panicSound != null) {
            panicSound.release();  // Release resources when activity is destroyed
            panicSound = null;
        }
    }
}