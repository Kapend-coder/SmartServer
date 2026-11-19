package com.smartsolution.smartserveurweb

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.smartsolution.smartserveurweb.databinding.ActivityMainBinding
import java.io.File

/**
 * Smart Serveur Web - Développé par Arsène KAPEND A KALEND (Smart Solution)
 * Lubumbashi / Kolwezi, RDC - Mission : Outils numériques sans internet
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServerRunning = false
    private val logStringBuilder = StringBuilder()

    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val action = it.action
                if (action == ServerService.ACTION_STATUS_UPDATE) {
                    val running = it.getBooleanExtra(ServerService.EXTRA_IS_RUNNING, false)
                    val ip = it.getStringExtra(ServerService.EXTRA_IP_ADDRESS) ?: ServerManager.getLocalIpAddress(this@MainActivity)
                    val port = it.getIntExtra(ServerService.EXTRA_PORT, 8080)
                    updateUiState(running, ip, port)
                } else if (action == ServerService.ACTION_LOG_APPEND) {
                    val logLine = it.getStringExtra(ServerService.EXTRA_LOG_LINE) ?: ""
                    appendLog(logLine)
                }
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val postNotificationsGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        if (!postNotificationsGranted) {
            Toast.makeText(this, "La permission de notification est recommandée pour le serveur en arrière-plan", Toast.LENGTH_LONG).show()
        }
        checkAndRequestManageStorage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        requestAppPermissions()
        requestBatteryOptimizationExemption()

        // Initialisation initiale du dossier htdocs
        ServerManager.initHtdocsDirectory(this)

        val currentIp = ServerManager.getLocalIpAddress(this)
        binding.tvServerUrl.text = "http://$currentIp:8080"
        binding.tvPortNumber.text = "8080"
        updateUiState(ServerService.isRunning, currentIp, 8080)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter().apply {
            addAction(ServerService.ACTION_STATUS_UPDATE)
            addAction(ServerService.ACTION_LOG_APPEND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statusReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statusReceiver, filter)
        }
        val currentIp = ServerManager.getLocalIpAddress(this)
        updateUiState(ServerService.isRunning, currentIp, 8080)
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(statusReceiver)
        } catch (e: Exception) {
            // Ignoré si déjà désenregistré
        }
    }

    private fun setupListeners() {
        // Bouton START
        binding.btnStart.setOnClickListener {
            if (!isServerRunning) {
                appendLog("[MAIN] Lancement de tous les services (Nginx, PHP, MariaDB, FTP)...")
                val serviceIntent = Intent(this, ServerService::class.java).apply {
                    action = ServerService.ACTION_START
                }
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                Toast.makeText(this, "Le serveur est déjà en marche", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton STOP
        binding.btnStop.setOnClickListener {
            if (isServerRunning) {
                appendLog("[MAIN] Arrêt de tous les services...")
                val serviceIntent = Intent(this, ServerService::class.java).apply {
                    action = ServerService.ACTION_STOP
                }
                startService(serviceIntent)
            } else {
                Toast.makeText(this, "Le serveur est déjà arrêté", Toast.LENGTH_SHORT).show()
            }
        }

        // Clic sur l'URL du serveur
        binding.tvServerUrl.setOnClickListener {
            val url = binding.tvServerUrl.text.toString()
            if (isServerRunning && url.startsWith("http")) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Impossible d'ouvrir le navigateur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Démarrez le serveur d'abord pour ouvrir l'URL", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton Ouvrir phpMyAdmin
        binding.btnOpenPhpMyAdmin.setOnClickListener {
            val ip = ServerManager.getLocalIpAddress(this)
            val pmaUrl = "http://$ip:8080/phpmyadmin"
            if (isServerRunning) {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pmaUrl))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Erreur ouverture phpMyAdmin : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Démarrez d'abord le serveur pour accéder à phpMyAdmin", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton Ouvrir htdocs
        binding.btnOpenHtdocs.setOnClickListener {
            openHtdocsFolder()
        }

        // Bouton Voir les logs
        binding.btnViewLogs.setOnClickListener {
            val intent = Intent(this, LogActivity::class.java).apply {
                putExtra(LogActivity.EXTRA_LOGS, logStringBuilder.toString())
            }
            startActivity(intent)
        }

        // Bouton Éditeur de texte
        binding.btnOpenEditor.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            startActivity(intent)
        }

        // Bouton À propos
        binding.btnAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUiState(running: Boolean, ip: String, port: Int) {
        isServerRunning = running
        if (running) {
            binding.tvStatus.text = "En marche"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.green_primary))
            binding.btnStart.isEnabled = false
            binding.btnStart.alpha = 0.5f
            binding.btnStop.isEnabled = true
            binding.btnStop.alpha = 1.0f
            binding.tvServerUrl.text = "http://$ip:$port"
            binding.tvPortNumber.text = port.toString()
        } else {
            binding.tvStatus.text = "Arrêté"
            binding.tvStatus.setTextColor(ContextCompat.getColor(this, R.color.red_primary))
            binding.btnStart.isEnabled = true
            binding.btnStart.alpha = 1.0f
            binding.btnStop.isEnabled = false
            binding.btnStop.alpha = 0.5f
            binding.tvServerUrl.text = "http://$ip:$port"
            binding.tvPortNumber.text = port.toString()
        }
    }

    private fun appendLog(line: String) {
        runOnUiThread {
            logStringBuilder.append(line).append("\n")
            // Limiter la taille du buffer pour éviter les saturations mémoire
            if (logStringBuilder.length > 50000) {
                logStringBuilder.delete(0, 10000)
            }
            binding.tvLogsStream.text = logStringBuilder.toString()
            binding.scrollViewLogs.post {
                binding.scrollViewLogs.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
    }

    private fun openHtdocsFolder() {
        val htdocsDir = ServerManager.getHtdocsDir(this)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = Uri.parse("content://com.android.externalstorage.documents/document/primary:SmartServeurWeb/htdocs")
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Dossier htdocs : ${htdocsDir.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkAndRequestManageStorage()
        }
    }

    private fun checkAndRequestManageStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Accès au stockage requis")
                    .setMessage("Smart Serveur Web a besoin d'accéder au dossier /SmartServeurWeb/htdocs pour héberger vos fichiers PHP, HTML, CSS et bases de données MariaDB sans interruption.")
                    .setPositiveButton("Autoriser") { _, _ ->
                        try {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                data = Uri.parse("package:$packageName")
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                            startActivity(intent)
                        }
                    }
                    .setNegativeButton("Plus tard", null)
                    .show()
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Ignoré
                }
            }
        }
    }
}
