package com.smartsolution.smartserveurweb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.smartsolution.smartserveurweb.databinding.ActivityAboutBinding

/**
 * Page À Propos avec les informations développeur intégrées sans modification.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "À propos de Smart Serveur Web"

        // Remplissage avec les données exactes
        binding.tvDevName.text = "Arsène KAPEND A KALEND"
        binding.tvDevCompany.text = "Smart Solution"
        binding.tvDevPhone.text = "+243 977 811 446"
        binding.tvDevLocation.text = "Lubumbashi / Kolwezi, RDC"
        binding.tvDevMission.text = "Créer des outils numériques qui marchent SANS INTERNET"
        binding.tvDevWebsite.text = "https://smartsolution-jq7ceh5rya-nw.a.run.app"

        // Actions de contact
        binding.btnCallPhone.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+243977811446"))
            startActivity(intent)
        }

        binding.btnWhatsApp.setOnClickListener {
            val url = "https://wa.me/243977811446"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        binding.btnOpenWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://smartsolution-jq7ceh5rya-nw.a.run.app"))
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
