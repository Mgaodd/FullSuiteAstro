package com.example.astrosuite;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

public class Permissions extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 122;
    private TextView permsTxt;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        permsTxt = findViewById(R.id.permsText);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            doThings();
            return;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            doThings();
            return;

        }
        onFail();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            doThings();
            return;
        }
        onFail();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (Arrays.asList(grantResults).contains(PackageManager.PERMISSION_DENIED)) {
                onFail();
                finish();
                System.exit(0);
            } else if (Arrays.asList(grantResults).contains(PackageManager.PERMISSION_GRANTED)) {
                doThings(); //call your dependent logic
            }
        }
    }

    private void onFail(){
        this.runOnUiThread(() -> Toast.makeText(getApplicationContext(), "Sorry, this requires location permissions!", Toast.LENGTH_LONG).show());
        permsTxt.setText("Sorry! I require location permissions.  And it seems you have denied me.");

    }

    private void doThings() {
        Intent intent = new Intent(this, ShowSensors.class);
        this.startActivity(intent);

    }
}