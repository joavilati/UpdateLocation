package com.example.updatelocation

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

private const val TAG = "LocationUpdater"

class LocationUpdater :  LifecycleService(){

    companion object {
        private const val NOTIFICATION_TITLE = "Location Updater"
        private const val RADIUS: Float = 180F
        private var count = 0
        private var geoAlreadyInitialized = false
        private const val TRACKING_CHANNEL = "tracking_channel"
        private const val TRACKING_NOTIFICATION_ID = 1
        private val exitFromGeofence = MutableLiveData(false)
        var isTrackingRider: Boolean = false
            private set

        fun startService(context: Context) {
            val startIntent = Intent(context, LocationUpdater::class.java)
            ContextCompat.startForegroundService(context, startIntent)
            isTrackingRider = true
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationUpdater::class.java)
            context.stopService(stopIntent)
            isTrackingRider = false
        }

        class GeofenceReceiver : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                val geofencingEvent = GeofencingEvent.fromIntent(intent)
                if (geofencingEvent.hasError()) {
                    return
                }
                when (geofencingEvent.geofenceTransition) {

                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        geofencingEvent.triggeringGeofences.forEach {
                            Log.d(TAG, "NINJA EXIT geo ${it.requestId} raio $RADIUS")
                        }
                        exitFromGeofence.postValue(true)
                    }

                    Geofence.GEOFENCE_TRANSITION_ENTER -> {
                        geofencingEvent.triggeringGeofences.forEach {
                            Log.d(TAG, "NINJA ENTER geo ${it.requestId} raio ${RADIUS}")
                        }
                    }

                    Geofence.GEOFENCE_TRANSITION_DWELL -> {

                    }
                }
            }
        }
    }

}