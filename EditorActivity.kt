package com.smartsolution.smartserveurweb

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.smartsolution.smartserveurweb.databinding.ActivityEditorBinding
import java.io.File

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding
    private var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Éditeur de code htdocs"

        val htdocs = ServerManager.getHtdocsDir(this)
        val defaultFile = File(htdocs, "index.php")
        if (defaultFile.exists()) {
            loadFile(defaultFile)
        }

        binding.btnSaveFile.setOnClickListener {
            saveCurrentFile()
        }

        binding.btnOpenFile.setOnClickListener {
            showFileSelectionDialog()
        }

        binding.btnNewFile.setOnClickListener {
            showNewFileDialog()
        }
    }

    private fun loadFile(file: File) {
        currentFile = file
        binding.tvCurrentFileName.text = file.name
        binding.etCodeContent.setText(file.readText())
        Toast.makeText(this, "Fichier chargé : ${file.name}", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentFile() {
        val file = currentFile
        if (file == null) {
            Toast.makeText(this, "Aucun fichier ouvert", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            file.writeText(binding.etCodeContent.text.toString())
            Toast.makeText(this, "Fichier ${file.name} sauvegardé !", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur sauvegarde : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showFileSelectionDialog() {
        val htdocs = ServerManager.getHtdocsDir(this)
        val files = htdocs.listFiles()?.filter { it.isFile } ?: emptyList()
        val fileNames = files.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Sélectionner un fichier dans htdocs")
            .setItems(fileNames) { _, which ->
                loadFile(files[which])
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showNewFileDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "ex: script.php ou style.css"
        }
        AlertDialog.Builder(this)
            .setTitle("Nouveau fichier")
            .setView(input)
            .setPositiveButton("Créer") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    val htdocs = ServerManager.getHtdocsDir(this)
                    val newFile = File(htdocs, name)
                    if (!newFile.exists()) {
                        newFile.writeText("<?php\n// Smart Serveur Web - ${name}\n")
                    }
                    loadFile(newFile)
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
