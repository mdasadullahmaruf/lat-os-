package com.mdasadullahmaruf.latos

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Locale

class VoiceService : Service() {

    private val CHANNEL_ID = "lat_os_voice_channel"
    private val TAG = "LatOSVoice"
    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var intentEngine: IntentEngine
    private lateinit var deepLinkRouter: DeepLinkRouter
    private lateinit var accessibilityExecutor: AccessibilityExecutor

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        intentEngine = IntentEngine()
        deepLinkRouter = DeepLinkRouter(this)
        
        // Initialize speech recognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            setupRecognitionListener()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        // Start listening for voice commands
        startListening()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
                // Restart listening after a short delay
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (speechRecognizer != null) {
                        startListening()
                    }
                }, 1500)
            }

            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                // Restart listening on error
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (speechRecognizer != null) {
                        startListening()
                    }
                }, 2000)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val command = matches[0]
                    Log.d(TAG, "Recognized: $command")
                    processCommand(command)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun processCommand(command: String) {
        val intent = intentEngine.parseCommand(command)
        
        if (intent != null) {
            Log.d(TAG, "Action: ${intent.action}, Target: ${intent.target}, Query: ${intent.query}")
            
            when (intent.action) {
                "open_app" -> {
                    if (intent.target.isNotEmpty()) {
                        val success = deepLinkRouter.openApp(intent.target)
                        if (!success) {
                            // Try as package name directly
                            try {
                                val launchIntent = packageManager.getLaunchIntentForPackage(intent.target)
                                launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                launchIntent?.let { startActivity(it) }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to open app: ${e.message}")
                            }
                        }
                    }
                }
                
                "search" -> {
                    when {
                        intent.target.contains("youtube") -> deepLinkRouter.searchYouTube(intent.query)
                        intent.target.contains("chrome") || intent.target.contains("google") -> deepLinkRouter.searchGoogle(intent.query)
                        else -> deepLinkRouter.searchGoogle(intent.query)
                    }
                }
                
                "call" -> {
                    deepLinkRouter.callNumber(intent.query)
                }
                
                "tap", "type", "scroll", "media" -> {
                    // These require accessibility service
                    // For now, show a toast or notification
                    // Full implementation needs accessibility service connection
                    Log.d(TAG, "Accessibility action: ${intent.action} - ${intent.query}")
                }
            }
        } else {
            Log.d(TAG, "Could not understand command: $command")
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
            .setContentText("Listening for voice commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .build()
    }
}
