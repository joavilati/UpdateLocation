package com.example.updatelocation

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.updatelocation.databinding.ActivityMainBinding
import java.util.jar.Manifest

private const val LOCATION_REQUEST = 41

class MainActivity : AppCompatActivity() {

    private var isTracking = false

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnStartPauseTracking.setOnClickListener {

            val permissionFineLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            val permissionCoarseLocation = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
            if (
                    permissionFineLocation == PackageManager.PERMISSION_GRANTED &&
                    permissionCoarseLocation == PackageManager.PERMISSION_GRANTED
            ) {
                checkGPSIsON()
            } else {
                val shouldShowExplanatoryUI = shouldShowRequestPermissionRationale(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (shouldShowExplanatoryUI) {
                    AlertDialog.Builder(this).apply {
                        setPositiveButton("Thanks!") { dialog, _ ->
                            requestPermissionsNeeded()
                            dialog.dismiss()
                        }
                        setTitle("To continue, we need your permission to access your location")

                        setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                    }
                } else {
                    requestPermissionsNeeded()
                }

            }

        }
    }

    private fun requestPermissionsNeeded() {

        requestPermissions(
                arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_REQUEST
        )
    }

    private fun startOrPauseForeground() {

        if (isTracking) {
            isTracking = false
            LocationUpdater.stopService(this)
            binding.btnStartPauseTracking.text = "Start Location Updater"
        } else {
            isTracking = true
            LocationUpdater.startService(this)
            binding.btnStartPauseTracking.text = "Stop Location Updater"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_REQUEST -> {
                if (grantResults.isNotEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //TODO Ask to turn on gps
                } else {
                    checkGPSIsON()
                }
            }
        }
    }

    private fun checkGPSIsON() {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gpsEnabled = false
        var networkEnabled = false

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (e: Exception) {
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (e: Exception) {
        }

        if (!gpsEnabled && !networkEnabled) {
            // notify user
            AlertDialog.Builder(this)
                    .setMessage("GPS is OFF, please turn on to continue")
                    .setPositiveButton("Ok") { dialog, _ ->
                        startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                    }
                    .setNegativeButton("cancel") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show();
        } else {
            startOrPauseForeground()
        }
    }
}