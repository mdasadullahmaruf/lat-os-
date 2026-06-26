package com.mdasadullahmaruf.latos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.util.Log
import android.view.View
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private val TAG = "LatOS_Main"

    // UI Elements
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    
    private lateinit var btnGrantPermissions: Button
    private lateinit var btnStartListening: Button
    private lateinit var btnStopListening: Button
    private lateinit var btnScanApps: Button
    private lateinit var btnSendCommand: Button
    private lateinit var btnTestFinder: Button
    private lateinit var btnClearLog: Button
    
    private lateinit var etCommand: EditText
    private lateinit var etTestApp: EditText

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var intentEngine: IntentEngine
    private lateinit var deepLinkRouter: DeepLinkRouter
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d(TAG, "=== onCreate START ===")

        // Initialize engines FIRST
        intentEngine = IntentEngine()
        deepLinkRouter = DeepLinkRouter(this)
        Log.d(TAG, "Engines initialized")

        // Find ALL views
        findAllViews()
        Log.d(TAG, "Views found")

        // Set up ALL click listeners
        setupClickListeners()
        Log.d(TAG, "Listeners set up")

        // Pre-scan apps
        Thread {
            try {
                PackageMapper.refreshCache(this)
                Log.d(TAG, "App cache loaded")
            } catch (e: Exception) {
                Log.e(TAG, "Pre-scan failed: ${e.message}")
            }
        }.start()

        updateUI()
        addLog("Lat OS started. Ready.")
        Log.d(TAG, "=== onCreate END ===")
    }

    private fun findAllViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        
        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnStopListening = findViewById(R.id.btnStopListening)
        btnScanApps = findViewById(R.id.btnScanApps)
        btnSendCommand = findViewById(R.id.btnSendCommand)
        btnTestFinder = findViewById(R.id.btnTestFinder)
        btnClearLog = findViewById(R.id.btnClearLog)
        
        etCommand = findViewById(R.id.etCommand)
        etTestApp = findViewById(R.id.etTestApp)

        // Verify all found
        val views = listOf(
            "tvStatus" to tvStatus, "tvLog" to tvLog, "scrollLog" to scrollLog,
            "btnGrantPermissions" to btnGrantPermissions, "btnStartListening" to btnStartListening,
            "btnStopListening" to btnStopListening, "btnScanApps" to btnScanApps,
            "btnSendCommand" to btnSendCommand, "btnTestFinder" to btnTestFinder,
            "btnClearLog" to btnClearLog, "etCommand" to etCommand, "etTestApp" to etTestApp
        )
        
        for ((name, view) in views) {
            if (view == null) {
                Log.e(TAG, "CRITICAL: View '$name' is NULL!")
            } else {
                Log.d(TAG, "View '$name' OK")
            }
        }
    }

    private fun setupClickListeners() {
        // Permissions button
        btnGrantPermissions.setOnClickListener {
            Log.d(TAG, "CLICK: Grant Permissions")
            addLog("Opening permission settings...")
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100)
                }
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                    startActivity(intent)
                }
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Permission error: ${e.message}")
                addLog("ERROR: ${e.message}")
            }
        }

        // Start Voice Service
        btnStartListening.setOnClickListener {
            Log.d(TAG, "CLICK: Start Listening")
            addLog("Starting voice service...")
            try {
                val serviceIntent = Intent(this, VoiceService::class.java)
                startForegroundService(serviceIntent)
                isServiceRunning = true
                addLog("Voice service started")
                updateUI()
                Toast.makeText(this, "Voice service started", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Start service error: ${e.message}")
                addLog("ERROR starting service: ${e.message}")
            }
        }

        // Stop Voice Service
        btnStopListening.setOnClickListener {
            Log.d(TAG, "CLICK: Stop Listening")
            addLog("Stopping voice service...")
            try {
                val serviceIntent = Intent(this, VoiceService::class.java)
                stopService(serviceIntent)
                isServiceRunning = false
                addLog("Voice service stopped")
                updateUI()
            } catch (e: Exception) {
                Log.e(TAG, "Stop service error: ${e.message}")
                addLog("ERROR stopping service: ${e.message}")
            }
        }

        // Scan Apps
        btnScanApps.setOnClickListener {
            Log.d(TAG, "CLICK: Scan Apps")
            addLog("=== SCANNING APPS ===")
            
            Thread {
                try {
                    val apps = PackageMapper.refreshCache(this)
                    val appStrings = apps.map { "${it.first} -> ${it.second}" }.sorted()
                    
                    handler.post {
                        addLog("Found ${apps.size} apps:")
                        appStrings.take(20).forEach { addLog(it) }
                        if (apps.size > 20) {
                            addLog("... and ${apps.size - 20} more")
                        }
                        addLog("=== SCAN DONE ===")
                    }
                } catch (e: Exception) {
                    handler.post {
                        addLog("SCAN ERROR: ${e.message}")
                        Log.e(TAG, "Scan error", e)
                    }
                }
            }.start()
        }

        // Run Test Command
        btnSendCommand.setOnClickListener {
            val command = etCommand.text.toString().trim()
            Log.d(TAG, "CLICK: Run Command '$command'")
            
            if (command.isEmpty()) {
                addLog("ERROR: Type a command first!")
                return@setOnClickListener
            }
            
            etCommand.text.clear()
            executeCommand(command)
        }

        // Keyboard enter key
        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val command = etCommand.text.toString().trim()
                if (command.isNotEmpty()) {
                    etCommand.text.clear()
                    executeCommand(command)
                }
                true
            } else {
                false
            }
        }

        // Test App Finder
        btnTestFinder.setOnClickListener {
            val appName = etTestApp.text.toString().trim()
            Log.d(TAG, "CLICK: Test Finder '$appName'")
            
            if (appName.isEmpty()) {
                addLog("ERROR: Type an app name first!")
                return@setOnClickListener
            }
            
            addLog("=== TEST FINDER ===")
            addLog("Looking for: '$appName'")
            
            Thread {
                try {
                    val pkg = PackageMapper.findPackage(this, appName)
                    handler.post {
                        if (pkg != null) {
                            addLog("FOUND: $pkg")
                            val installed = PackageMapper.isInstalled(this, pkg)
                            addLog("Installed: $installed")
                            if (installed) {
                                addLog("Launching...")
                                deepLinkRouter.openPackage(pkg)
                                addLog("Launched!")
                            }
                        } else {
                            addLog("NOT FOUND")
                        }
                        addLog("=== DONE ===")
                    }
                } catch (e: Exception) {
                    handler.post {
                        addLog("FINDER ERROR: ${e.message}")
                    }
                }
            }.start()
        }

        // Clear Log
        btnClearLog.setOnClickListener {
            Log.d(TAG, "CLICK: Clear Log")
            tvLog.text = ""
            addLog("Log cleared")
        }
    }

    private fun executeCommand(command: String) {
        addLog("=== COMMAND ===")
        addLog("Input: '$command'")

        try {
            val intent = intentEngine.parseCommand(command)

            if (intent == null) {
                addLog("PARSE FAILED")
                addLog("Try: 'open youtube' or 'search cats'")
                addLog("=== END ===")
                return
            }

            addLog("Action: ${intent.action}")
            addLog("Target: ${intent.target}")
            addLog("Query: ${intent.query}")

            when (intent.action) {
                "open_app" -> {
                    if (intent.target.isEmpty()) {
                        addLog("ERROR: No app name")
                        addLog("=== END ===")
                        return
                    }
                    
                    addLog("Finding app: ${intent.target}")
                    val (success, pkgName) = deepLinkRouter.openApp(intent.target)
                    
                    if (success) {
                        addLog("SUCCESS: Opened ${intent.target}")
                        addLog("Package: $pkgName")
                    } else {
                        addLog("FAILED: ${intent.target}")
                        addLog("Package result: $pkgName")
                    }
                    addLog("=== END ===")
                }

                "search" -> {
                    addLog("Searching: ${intent.query}")
                    if (intent.target.contains("youtube")) {
                        val success = deepLinkRouter.searchYouTube(intent.query)
                        addLog("YouTube search: ${if (success) "OK" else "FAILED"}")
                    } else {
                        deepLinkRouter.searchGoogle(intent.query)
                        addLog("Google search: OK")
                    }
                    addLog("=== END ===")
                }

                "call" -> {
                    deepLinkRouter.callNumber(intent.query)
                    addLog("Dialing: ${intent.query}")
                    addLog("=== END ===")
                }

                else -> {
                    addLog("Action '${intent.action}' not implemented yet")
                    addLog("=== END ===")
                }
            }
        } catch (e: Exception) {
            addLog("CRASH: ${e.message}")
            Log.e(TAG, "Execute crash", e)
        }
    }

    private fun updateUI() {
        val hasAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val hasOverlay = Settings.canDrawOverlays(this)

        if (hasAudio && hasOverlay) {
            btnStartListening.isEnabled = true
            btnGrantPermissions.text = "Permissions Granted"
            
            if (isServiceRunning) {
                tvStatus.text = "Voice service RUNNING"
                btnStartListening.visibility = View.GONE
                btnStopListening.visibility = View.VISIBLE
            } else {
                tvStatus.text = "Ready - tap Start or type command"
                btnStartListening.visibility = View.VISIBLE
                btnStopListening.visibility = View.GONE
            }
        } else {
            btnStartListening.isEnabled = false
            btnStopListening.visibility = View.GONE
            tvStatus.text = "Grant permissions first"
        }
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val currentText = tvLog.text.toString()
        tvLog.text = "$currentText\n[$time] $message"
        
        // Auto-scroll to bottom
        scrollLog.post {
            scrollLog.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUI()
    }
}
