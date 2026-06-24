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

class MainActivity : AppCompatActivity() {

    private lateinit var btnGrantPermissions: Button
    private lateinit var btnStartListening: Button
    private lateinit var btnStopListening: Button
    private lateinit var btnScanApps: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var etCommand: EditText
    private lateinit var btnSendCommand: Button
    private lateinit var btnClearLog: Button
    private lateinit var btnTestFinder: Button
    private lateinit var etTestApp: EditText

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var intentEngine: IntentEngine
    private lateinit var deepLinkRouter: DeepLinkRouter
    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        intentEngine = IntentEngine()
        deepLinkRouter = DeepLinkRouter(this)

        btnGrantPermissions = findViewById(R.id.btnGrantPermissions)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnStopListening = findViewById(R.id.btnStopListening)
        btnScanApps = findViewById(R.id.btnScanApps)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        etCommand = findViewById(R.id.etCommand)
        btnSendCommand = findViewById(R.id.btnSendCommand)
        btnClearLog = findViewById(R.id.btnClearLog)
        btnTestFinder = findViewById(R.id.btnTestFinder)
        etTestApp = findViewById(R.id.etTestApp)

        btnGrantPermissions.setOnClickListener {
            checkAndRequestPermissions()
        }

        btnStartListening.setOnClickListener {
            startVoiceService()
        }

        btnStopListening.setOnClickListener {
            stopVoiceService()
        }

        btnScanApps.setOnClickListener {
            scanInstalledApps()
        }

        btnSendCommand.setOnClickListener {
            val command = etCommand.text.toString().trim()
            if (command.isNotEmpty()) {
                executeCommand(command)
                etCommand.text.clear()
            }
        }

        etCommand.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                val command = etCommand.text.toString().trim()
                if (command.isNotEmpty()) {
                    executeCommand(command)
                    etCommand.text.clear()
                }
                true
            } else {
                false
            }
        }

        btnTestFinder.setOnClickListener {
            val appName = etTestApp.text.toString().trim()
            if (appName.isNotEmpty()) {
                testAppFinder(appName)
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

    private fun scanInstalledApps() {
        addLog("=== SCANNING ALL INSTALLED APPS ===")
        addLog("This may take a moment...")
        
        Thread {
            val apps = PackageMapper.refreshCache(this)
            val appStrings = apps.map { "${it.first} -> ${it.second}" }.sorted()
            
            handler.post {
                addLog("=== ALL APPS (${apps.size}) ===")
                appStrings.take(30).forEach { addLog(it) }
                if (apps.size > 30) {
                    addLog("... and ${apps.size - 30} more apps")
                }
                addLog("=== END OF LIST ===")
                addLog("Tip: Use 'Test App Finder' to check if a specific app can be found")
            }
        }.start()
    }

    private fun testAppFinder(appName: String) {
        addLog("=== TEST APP FINDER ===")
        addLog("Looking for: '$appName'")
        
        Thread {
            val pkg = PackageMapper.findPackage(this, appName)
            handler.post {
                if (pkg != null) {
                    addLog("FOUND: $pkg")
                    addLog("Testing launch...")
                    val success = PackageMapper.isInstalled(this, pkg)
                    addLog("Installed: $success")
                    if (success) {
                        addLog("Launching now...")
                        deepLinkRouter.openPackage(pkg)
                    }
                } else {
                    addLog("NOT FOUND: No app matching '$appName'")
                    addLog("Try scanning all apps first to see exact names")
                }
                addLog("=== END TEST ===")
            }
        }.start()
    }

    private fun executeCommand(command: String) {
        addLog("=== COMMAND ===")
        addLog("You typed: \"$command\"")

        val intent = intentEngine.parseCommand(command)

        if (intent != null) {
            addLog("Parsed: action=${intent.action}, target=${intent.target}, query=${intent.query}")

            when (intent.action) {
                "open_app" -> {
                    if (intent.target.isNotEmpty()) {
                        addLog("Searching for app: ${intent.target}")
                        
                        Thread {
                            val (success, pkgName) = deepLinkRouter.openApp(intent.target)
                            handler.post {
                                if (success) {
                                    addLog("SUCCESS: Opened ${intent.target} ($pkgName)")
                                } else {
                                    addLog("FAILED: Could not open ${intent.target}")
                                    addLog("Package found: $pkgName")
                                    addLog("Tip: Try 'Test App Finder' with the exact app name")
                                }
                                addLog("=== END ===")
                            }
                        }.start()
                    } else {
                        addLog("FAILED: No app name detected in command")
                        addLog("Try: 'Open YouTube' or 'Open Chrome'")
                        addLog("=== END ===")
                    }
                }

                "search" -> {
                    addLog("Searching for: ${intent.query}")
                    when {
                        intent.target.contains("youtube") -> {
                            val success = deepLinkRouter.searchYouTube(intent.query)
                            if (success) {
                                addLog("SUCCESS: Searching YouTube for '${intent.query}'")
                            } else {
                                addLog("FAILED: Could not search YouTube")
                            }
                        }
                        else -> {
                            deepLinkRouter.searchGoogle(intent.query)
                            addLog("SUCCESS: Searching Google for '${intent.query}'")
                        }
                    }
                    addLog("=== END ===")
                }

                "call" -> {
                    deepLinkRouter.callNumber(intent.query)
                    addLog("SUCCESS: Dialing ${intent.query}")
                    addLog("=== END ===")
                }

                "tap" -> {
                    addLog("INFO: Tap command requires Accessibility Service")
                    addLog("Target: ${intent.query}")
                    addLog("=== END ===")
                }

                "type" -> {
                    addLog("INFO: Type command requires Accessibility Service")
                    addLog("Text: ${intent.query}")
                    addLog("=== END ===")
                }

                "scroll" -> {
                    addLog("INFO: Scroll command requires Accessibility Service")
                    addLog("Direction: ${intent.query}")
                    addLog("=== END ===")
                }

                else -> {
                    addLog("INFO: Command '${intent.action}' not yet implemented")
                    addLog("=== END ===")
                }
            }
        } else {
            addLog("FAILED: Could not understand command")
            addLog("Try: 'Open YouTube' or 'Search cats'")
            addLog("=== END ===")
        }
    }

    private fun addLog(message: String) {
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val currentText = tvLog.text.toString()
        val newLine = "[$time] $message\n"
        tvLog.text = currentText + newLine

        handler.post {
            scrollLog.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateUI()
    }
}
