package com.mdasadullahmaruf.latos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrantPermissions: Button
    private lateinit var btnStartListening: Button
    private lateinit var btnStopListening: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var etCommand: EditText
    private lateinit var btnSendCommand: ImageButton
    private lateinit var btnClearLog: Button

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var intentEngine: IntentEngine
    private lateinit var deepLinkRouter: DeepLinkRouter
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize engines
        intentEngine = IntentEngine()
        deepLinkRouter = DeepLinkRouter(this)

        // Find views
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnStopListening = findViewById(R.id.btnStopListening)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        etCommand = findViewById(R.id.etCommand)
        btnSendCommand = findViewById(R.id.btnSendCommand)
        btnClearLog = findViewById(R.id.btnClearLog)

        // Button listeners
        btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnStartListening.setOnClickListener {
            startVoiceService()
        }

        btnStopListening.setOnClickListener {
            stopVoiceService()
        }

        btnSendCommand.setOnClickListener {
            val command = etCommand.text.toString().trim()
            if (command.isNotEmpty()) {
                executeCommand(command)
                etCommand.text.clear()
            }
        }

        btnClearLog.setOnClickListener {
            tvLog.text = ""
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

        if (hasAudio && hasOverlay) {
            btnStartListening.isEnabled = true
            btnGrantPermissions.text = "Permissions Granted"
            if (isServiceRunning) {
                tvStatus.text = "Voice service is RUNNING\nFloating bubble is active"
                btnStartListening.visibility = Button.GONE
                btnStopListening.visibility = Button.VISIBLE
            } else {
                tvStatus.text = "Ready! Tap 'Start Voice Control' or type command below"
                btnStartListening.visibility = Button.VISIBLE
                btnStopListening.visibility = Button.GONE
            }
        } else {
            btnStartListening.isEnabled = false
            btnStopListening.visibility = Button.GONE
            tvStatus.text = "Grant all permissions to continue"
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
        }

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceService::class.java)
        startForegroundService(serviceIntent)
        isServiceRunning = true
        tvStatus.text = "Voice service starting...\nLook for floating bubble at top"
        addLog("Voice service started")
        updateUI()
        
        handler.postDelayed({
            moveTaskToBack(true)
        }, 2000)
    }

    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoiceService::class.java)
        stopService(serviceIntent)
        isServiceRunning = false
        tvStatus.text = "Voice service stopped"
        addLog("Voice service stopped")
        updateUI()
    }

    // Manual command execution for testing
    private fun executeCommand(command: String) {
        addLog("You typed: \"$command\"")
        
        val intent = intentEngine.parseCommand(command)
        
        if (intent != null) {
            addLog("Parsed: action=${intent.action}, target=${intent.target}, query=${intent.query}")
            
            when (intent.action) {
                "open_app" -> {
                    if (intent.target.isNotEmpty()) {
                        val success = deepLinkRouter.openApp(intent.target)
                        if (success) {
                            addLog("SUCCESS: Opened app ${intent.target}")
                        } else {
                            // Try direct package launch
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(intent.target)
                                if (launchIntent != null) {
                                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(launchIntent)
                                    addLog("SUCCESS: Launched ${intent.target}")
                                } else {
                                    addLog("FAILED: App not found - ${intent.target}")
                                }
                            } catch (e: Exception) {
                                addLog("ERROR: ${e.message}")
                            }
                        }
                    } else {
                        addLog("FAILED: No app specified")
                    }
                }
                
                "search" -> {
                    when {
                        intent.target.contains("youtube") -> {
                            deepLinkRouter.searchYouTube(intent.query)
                            addLog("SUCCESS: Searching YouTube for '${intent.query}'")
                        }
                        intent.target.contains("chrome") || intent.target.contains("google") -> {
                            deepLinkRouter.searchGoogle(intent.query)
                            addLog("SUCCESS: Searching Google for '${intent.query}'")
                        }
                        else -> {
                            deepLinkRouter.searchGoogle(intent.query)
                            addLog("SUCCESS: Searching Google for '${intent.query}'")
                        }
                    }
                }
                
                "call" -> {
                    deepLinkRouter.callNumber(intent.query)
                    addLog("SUCCESS: Dialing ${intent.query}")
                }
                
                "tap" -> {
                    addLog("INFO: Tap command requires Accessibility Service")
                    addLog("Target: ${intent.query}")
                }
                
                "type" -> {
                    addLog("INFO: Type command requires Accessibility Service")
                    addLog("Text: ${intent.query}")
                }
                
                "scroll" -> {
                    addLog("INFO: Scroll command requires Accessibility Service")
                    addLog("Direction: ${intent.query}")
                }
                
                else -> {
                    addLog("INFO: Command '${intent.action}' not yet implemented")
                }
            }
        } else {
            addLog("FAILED: Could not understand command")
            addLog("Try: 'Open YouTube' or 'Search cats on Google'")
        }
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val currentText = tvLog.text.toString()
        val newLine = "[$time] $message\n"
        tvLog.text = currentText + newLine
        
        // Auto scroll to bottom
        handler.post {
            scrollLog.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUI()
    }
}
