package com.smartsolution.smartserveurweb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.smartsolution.smartserveurweb.databinding.ActivityLogBinding

class LogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogBinding

    companion object {
        const val EXTRA_LOGS = "extra_logs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Journaux d'exécution"

        val logs = intent.getStringExtra(EXTRA_LOGS) ?: "Aucun log pour le moment."
        binding.tvFullLogs.text = logs

        binding.btnCopyLogs.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SmartServeurWeb Logs", binding.tvFullLogs.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Logs copiés dans le presse-papier !", Toast.LENGTH_SHORT).show()
        }

        binding.btnClearLogs.setOnClickListener {
            binding.tvFullLogs.text = ""
            Toast.makeText(this, "Logs effacés de l'affichage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
