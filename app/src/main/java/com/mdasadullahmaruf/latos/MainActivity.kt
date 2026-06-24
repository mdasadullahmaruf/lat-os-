package com.mdasadullahmaruf.latos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrantPermissions: Button
    private lateinit var btnStartListening: Button
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnStartListening = findViewById(R.id.btnStartListening)
        tvStatus = findViewById(R.id.tvStatus)

        btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnStartListening.setOnClickListener {
            startVoiceService()
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Settings.canDrawOverlays(this)
        // Accessibility is checked via system settings, not runtime permission

        if (hasAudio && hasOverlay) {
            btnStartListening.isEnabled = true
            btnGrantPermissions.text = "Permissions Granted"
            tvStatus.text = "Ready! Tap 'Start Voice Control'"
        } else {
            btnStartListening.isEnabled = false
            tvStatus.text = "Grant all permissions to continue"
        }
    }

    private fun checkAndRequestPermissions() {
        // Request microphone
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        // Request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // Guide to accessibility settings
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceService::class.java)
        startForegroundService(serviceIntent)
        tvStatus.text = "Voice service running..."
    }
}
