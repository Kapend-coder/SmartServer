package com.smartsolution.smartserveurweb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service Foreground maintenant en vie tous les serveurs :
 * Nginx (8080), PHP 8.2 (9000), MariaDB (3306), FTP (2121)
 */
class ServerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val CHANNEL_ID = "SmartServeurWebChannel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.smartsolution.smartserveurweb.ACTION_START"
        const val ACTION_STOP = "com.smartsolution.smartserveurweb.ACTION_STOP"
        const val ACTION_STATUS_UPDATE = "com.smartsolution.smartserveurweb.ACTION_STATUS_UPDATE"
        const val ACTION_LOG_APPEND = "com.smartsolution.smartserveurweb.ACTION_LOG_APPEND"

        const val EXTRA_IS_RUNNING = "extra_is_running"
        const val EXTRA_IP_ADDRESS = "extra_ip_address"
        const val EXTRA_PORT = "extra_port"
        const val EXTRA_LOG_LINE = "extra_log_line"

        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SmartServeurWeb::WakeLock")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_START) {
            if (!isRunning) {
                startForeground(NOTIFICATION_ID, createNotification("Initialisation des services..."))
                wakeLock?.acquire(10 * 60 * 1000L /* 10 minutes ou infini */)
                startServers()
            }
        } else if (action == ACTION_STOP) {
            stopServers()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_STICKY
    }

    private fun startServers() {
        serviceScope.launch {
            isRunning = true
            broadcastLog("[SERVICE] Démarrage du bundle Smart Serveur Web...")

            // Initialisation de l'environnement (fichiers, dossiers htdocs, assets, binaires)
            ServerManager.initializeEnvironment(this@ServerService) { logMsg ->
                broadcastLog(logMsg)
            }

            // Démarrage MariaDB (port 3306)
            ServerManager.startMariaDb(this@ServerService) { logMsg ->
                broadcastLog(logMsg)
            }

            // Démarrage PHP-FPM 8.2 (port 9000 ou socket)
            ServerManager.startPhpFpm(this@ServerService) { logMsg ->
                broadcastLog(logMsg)
            }

            // Démarrage Nginx (port 8080)
            ServerManager.startNginx(this@ServerService) { logMsg ->
                broadcastLog(logMsg)
            }

            // Démarrage Serveur FTP (port 2121)
            ServerManager.startFtpServer(this@ServerService) { logMsg ->
                broadcastLog(logMsg)
            }

            val ip = ServerManager.getLocalIpAddress(this@ServerService)
            broadcastLog("[SERVICE] TOUS LES SERVICES SONT EN MARCHE !")
            broadcastLog("[SERVICE] Web : http://$ip:8080 | phpMyAdmin : http://$ip:8080/phpmyadmin")
            broadcastLog("[SERVICE] MariaDB : $ip:3306 (root / root123) | FTP : $ip:2121 (serveur / serveur123)")

            updateNotification("En marche - http://$ip:8080")
            broadcastStatus(true, ip, 8080)
        }
    }

    private fun stopServers() {
        serviceScope.launch {
            broadcastLog("[SERVICE] Arrêt de Nginx...")
            ServerManager.stopNginx()

            broadcastLog("[SERVICE] Arrêt de PHP-FPM...")
            ServerManager.stopPhpFpm()

            broadcastLog("[SERVICE] Arrêt de MariaDB...")
            ServerManager.stopMariaDb()

            broadcastLog("[SERVICE] Arrêt du serveur FTP...")
            ServerManager.stopFtpServer()

            isRunning = false
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (e: Exception) {
                // Ignore
            }

            val ip = ServerManager.getLocalIpAddress(this@ServerService)
            broadcastLog("[SERVICE] Tous les serveurs ont été arrêtés proprement.")
            broadcastStatus(false, ip, 8080)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Smart Serveur Web Notification"
            val descriptionText = "Affiche l'état d'exécution du serveur Web, MariaDB et FTP"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val stopIntent = Intent(this, ServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Smart Serveur Web - Smart Solution")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_server_notification)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "ARRÊTER", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun broadcastStatus(running: Boolean, ip: String, port: Int) {
        val intent = Intent(ACTION_STATUS_UPDATE).apply {
            putExtra(EXTRA_IS_RUNNING, running)
            putExtra(EXTRA_IP_ADDRESS, ip)
            putExtra(EXTRA_PORT, port)
        }
        sendBroadcast(intent)
    }

    private fun broadcastLog(msg: String) {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedLog = "[${timeFormat.format(Date())}] $msg"
        val intent = Intent(ACTION_LOG_APPEND).apply {
            putExtra(EXTRA_LOG_LINE, formattedLog)
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        isRunning = false
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
