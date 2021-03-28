package com.example.updatelocation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.updatelocation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var isTracking = false

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnStartPauseTracking.setOnClickListener {
            it as Button
            if(isTracking) {
                isTracking = false
                LocationUpdater.stopService(this)

                it.text = "Start Location Updater"
            } else {
                isTracking = true
                LocationUpdater.startService(this)
                it.text = "Stop Location Updater"
            }
        }

    }
}