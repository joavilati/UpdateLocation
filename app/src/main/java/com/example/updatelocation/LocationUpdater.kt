package com.example.updatelocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*

private const val TAG = "LocationUpdater"

class LocationUpdater : LifecycleService() {

    private lateinit var locationCallBack: LocationCallback
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var pendingBroadcastIntent: PendingIntent
    private var geoId = ""
    private lateinit var handlerThread : HandlerThread

    companion object {
        private const val NOTIFICATION_TITLE = "Location Updater"
        private const val RADIUS: Float = 180F
        private const val TRACKING_CHANNEL = "tracking_channel"
        private const val TRACKING_NOTIFICATION_ID = 1
        private val exitFromGeofence = MutableLiveData(false)

        fun startService(context: Context) {
            val startIntent = Intent(context, LocationUpdater::class.java)
            ContextCompat.startForegroundService(context, startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, LocationUpdater::class.java)
            context.stopService(stopIntent)
        }

        //region GEOFENCE
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
//        endregion
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = LocationServices.getFusedLocationProviderClient(this)
        geofencingClient = LocationServices.getGeofencingClient(this)
        val intent = Intent(this, GeofenceReceiver::class.java)
        pendingBroadcastIntent = PendingIntent.getBroadcast(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        locationCallBack = createLocationCallBack()

        createObservers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        createNotificationChannel()
        startNotificationService()
        return START_NOT_STICKY
    }

    private fun createObservers() {
        exitFromGeofence.observe(this) {

            updateLocation()

        }
    }

    private fun updateLocation() {
        val request = LocationRequest.create().apply {
            interval = 15000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            isWaitForAccurateLocation = true
        }

        val permissionFineLocation = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

        val builder = LocationSettingsRequest.Builder().apply {
            addLocationRequest(request)
        }

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.apply {
            addOnSuccessListener {
                locationCallBack = createLocationCallBack()
                handlerThread = HandlerThread("RequestLocation")
                handlerThread.start()
                if (permissionFineLocation == PackageManager.PERMISSION_GRANTED) {
                    locationClient.requestLocationUpdates(
                            request,
                            locationCallBack,
                            handlerThread.looper
                    ).apply {
                        addOnSuccessListener {
                            Log.d(TAG, "REQUEST LOCATION SUCCESS")
                        }
                        addOnFailureListener {
                            Log.d(TAG, "REQUEST LOCATION FAILURE")
                        }
                        addOnCanceledListener {
                            Log.d(TAG, "REQUEST LOCATION CANCELED")
                        }
                    }
                }
            }
        }
    }

    private fun createLocationCallBack() = object : LocationCallback() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onLocationResult(locationResult: LocationResult) {
            val location: Location = locationResult.lastLocation
            val accuracy = location.verticalAccuracyMeters
            Log.d(
                    TAG,
                    "LOCATION RESULT lat = ${location.latitude} lng = ${location.longitude} accuracy = $accuracy"
            )
            createNewGeofence(location)
        }
    }

    @SuppressLint("MissingPermission")
    private fun createNewGeofence(location: Location) {
        handlerThread.quit()
        removeActualGeofence()
        geoId = "geo_id"
        val geofence = getGeofence(location)
        val geofencingRequest = getGeofenceRequest(geofence)
        initGeofence(geofencingRequest)
    }

    private fun getGeofenceRequest(geofence: Geofence) =
            GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                    .addGeofence(geofence)
                    .build()

    private fun getGeofence(location: Location) = Geofence.Builder()
            .setCircularRegion(location.latitude, location.longitude, RADIUS)
            .setRequestId(geoId)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setNotificationResponsiveness(0)
            .build()

    @SuppressLint("MissingPermission")
    private fun initGeofence(geofencingRequest: GeofencingRequest) {
        geofencingClient.addGeofences(geofencingRequest, pendingBroadcastIntent)
                .addOnSuccessListener {
                    Log.d(TAG, "ADD GEOFENCE SUCCESS")
                }
                .addOnFailureListener {
                    Log.d(TAG, "ADD GEOFENCE FAILS")
                }
    }

    private fun removeActualGeofence() {
        geofencingClient.removeGeofences(mutableListOf(geoId))
                .addOnFailureListener {
                    Log.d(TAG, "REMOVE GEOFENCE FAIL")
                }
                .addOnSuccessListener {
                    Log.d(TAG, "REMOVE GEOFENCE SUCCESS")
                }
    }

    //region NOTIFICATION SERVICE
    private fun startNotificationService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                0
        )

        val notification = NotificationCompat.Builder(this, TRACKING_CHANNEL)
                .setChannelId(TRACKING_CHANNEL)
                .setContentTitle(NOTIFICATION_TITLE)
                .setSmallIcon(R.drawable.ic_baseline_person_pin_circle_24)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setContentIntent(pendingIntent)
                .build()

        startForeground(TRACKING_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                    TRACKING_CHANNEL,
                    "Foreground Location Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            assert(manager != null)
            manager.createNotificationChannel(notificationChannel)
        }
    }
//    endregion
}