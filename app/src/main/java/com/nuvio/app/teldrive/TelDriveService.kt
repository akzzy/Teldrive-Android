package com.nuvio.app.teldrive

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TelDriveService : Service() {

    companion object {
        private const val CHANNEL_ID = "TelDriveServiceChannel"
        private const val NOTIFICATION_ID = 2341
        
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        
        const val EXTRA_SUPABASE_DSN = "EXTRA_SUPABASE_DSN"
        const val EXTRA_JWT_SECRET = "EXTRA_JWT_SECRET"
        const val EXTRA_PORT = "EXTRA_PORT"
        const val EXTRA_TG_APP_ID = "EXTRA_TG_APP_ID"
        const val EXTRA_TG_APP_HASH = "EXTRA_TG_APP_HASH"
        const val EXTRA_TG_UPLOADS_ENCRYPTION_KEY = "EXTRA_TG_UPLOADS_ENCRYPTION_KEY"
        
        fun startService(
            context: Context,
            supabaseDsn: String,
            jwtSecret: String,
            port: Int = 8080,
            tgAppId: String = "",
            tgAppHash: String = "",
            tgUploadsEncryptionKey: String = ""
        ) {
            val intent = Intent(context, TelDriveService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SUPABASE_DSN, supabaseDsn)
                putExtra(EXTRA_JWT_SECRET, jwtSecret)
                putExtra(EXTRA_PORT, port)
                putExtra(EXTRA_TG_APP_ID, tgAppId)
                putExtra(EXTRA_TG_APP_HASH, tgAppHash)
                putExtra(EXTRA_TG_UPLOADS_ENCRYPTION_KEY, tgUploadsEncryptionKey)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, TelDriveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        if (action == ACTION_START) {
            val supabaseDsn = intent.getStringExtra(EXTRA_SUPABASE_DSN) ?: ""
            val jwtSecret = intent.getStringExtra(EXTRA_JWT_SECRET) ?: "default_secret_string"
            val port = intent.getIntExtra(EXTRA_PORT, 8080)
            val tgAppId = intent.getStringExtra(EXTRA_TG_APP_ID) ?: ""
            val tgAppHash = intent.getStringExtra(EXTRA_TG_APP_HASH) ?: ""
            val tgUploadsEncryptionKey = intent.getStringExtra(EXTRA_TG_UPLOADS_ENCRYPTION_KEY) ?: ""
            
            createNotificationChannel()
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            
            // Start TelDrive Daemon
            TelDriveManager.startServer(
                context = this,
                supabaseDsn = supabaseDsn,
                jwtSecret = jwtSecret,
                port = port,
                tgAppId = tgAppId,
                tgAppHash = tgAppHash,
                tgUploadsEncryptionKey = tgUploadsEncryptionKey
            )
        } else if (action == ACTION_STOP) {
            TelDriveManager.stopServer()
            stopForeground(true)
            stopSelf()
        }
        
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        TelDriveManager.stopServer()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotification(): Notification {
        val notificationIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nuvio Cloud Engine")
            .setContentText("TelDrive local gateway is running...")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "TelDrive Engine Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
