package com.mdasadullahmaruf.latos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceService : Service() {

    private val CHANNEL_ID = "lat_os_voice_channel"
    private val TAG = "LatOSVoice"
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var intentEngine: IntentEngine
    private lateinit var deepLinkRouter: DeepLinkRouter
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var statusText: TextView? = null
    private var micIcon: ImageView? = null
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        intentEngine = IntentEngine()
        deepLinkRouter = DeepLinkRouter(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupRecognitionListener()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        showFloatingUI()
        startListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
        removeFloatingUI()
    }

    private fun showFloatingUI() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 100
        }

        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_status, null)
        statusText = floatingView?.findViewById(R.id.tvFloatingStatus)
        micIcon = floatingView?.findViewById(R.id.ivFloatingMic)
        
        windowManager.addView(floatingView, params)
        updateStatus("Tap mic and speak", R.color.white)
    }

    private fun removeFloatingUI() {
        floatingView?.let {
            windowManager.removeView(it)
            floatingView = null
        }
    }

    private fun updateStatus(text: String, colorRes: Int) {
        handler.post {
            statusText?.text = text
            statusText?.setTextColor(getColor(colorRes))
        }
    }

    private fun pulseMic() {
        handler.post {
            micIcon?.animate()?.scaleX(1.3f)?.scaleY(1.3f)?.setDuration(300)?.withEndAction {
                micIcon?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(300)?.start()
            }?.start()
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                updateStatus("🎤 Listening...", R.color.teal_200)
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
                updateStatus("🔴 Hearing you...", R.color.teal_200)
                pulseMic()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Visual feedback based on volume could go here
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                updateStatus("⏳ Processing...", R.color.purple_200)
            }

            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "No mic permission"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Busy, retrying..."
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Error $error"
                }
                Log.e(TAG, "Error: $errorMsg")
                updateStatus("❌ $errorMsg", android.R.color.holo_red_light)
                
                handler.postDelayed({
                    updateStatus("🎤 Tap to speak", R.color.white)
                    startListening()
                }, 2000)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    Log.d(TAG, "Heard: $command")
                    updateStatus("✅ Heard: \"$command\"", R.color.teal_200)
                    processCommand(command)
                } else {
                    updateStatus("❌ No speech detected", android.R.color.holo_red_light)
                    handler.postDelayed({ startListening() }, 1500)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!partial.isNullOrEmpty()) {
                    updateStatus("...${partial[0]}", R.color.purple_200)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processCommand(command: String) {
        val intent = intentEngine.parseCommand(command)
        
        if (intent != null) {
            updateStatus("🚀 ${intent.action}: ${intent.query}", R.color.teal_200)
            
            when (intent.action) {
                "open_app" -> {
                    if (intent.target.isNotEmpty()) {
                        handler.postDelayed({
                            val success = deepLinkRouter.openApp(intent.target)
                            if (success) {
                                updateStatus("✅ Opened!", R.color.teal_200)
                                showToast("Opened ${intent.target}")
                            } else {
                                updateStatus("❌ App not found", android.R.color.holo_red_light)
                            }
                            handler.postDelayed({ startListening() }, 2000)
                        }, 500)
                    }
                }
                
                "search" -> {
                    handler.postDelayed({
                        when {
                            intent.target.contains("youtube") -> {
                                deepLinkRouter.searchYouTube(intent.query)
                                updateStatus("✅ Searching YouTube", R.color.teal_200)
                            }
                            else -> {
                                deepLinkRouter.searchGoogle(intent.query)
                                updateStatus("✅ Searching Google", R.color.teal_200)
                            }
                        }
                        handler.postDelayed({ startListening() }, 2000)
                    }, 500)
                }
                
                "call" -> {
                    handler.postDelayed({
                        deepLinkRouter.callNumber(intent.query)
                        updateStatus("✅ Dialing ${intent.query}", R.color.teal_200)
                        handler.postDelayed({ startListening() }, 2000)
                    }, 500)
                }
                
                else -> {
                    updateStatus("⚠️ Command not yet supported", android.R.color.holo_orange_light)
                    handler.postDelayed({ startListening() }, 2000)
                }
            }
        } else {
            updateStatus("❓ Didn't understand: \"$command\"", android.R.color.holo_orange_light)
            handler.postDelayed({
                updateStatus("🎤 Try: Open YouTube", R.color.white)
                startListening()
            }, 2500)
        }
    }

    private fun showToast(message: String) {
        handler.post {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lat OS Voice Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Running voice recognition in background"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Lat OS")
            .setContentText("Voice assistant active")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }
}
