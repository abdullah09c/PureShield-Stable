package com.abdullah09c.pureshield.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.abdullah09c.pureshield.R
import com.abdullah09c.pureshield.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_about)

        binding.tvVersion.text = getString(R.string.about_version)
        binding.tvDescription.text = getString(R.string.about_description)

        binding.btnGithub.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, getString(R.string.about_nanitex_url).toUri()))
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            val policyUrl = getString(R.string.privacy_policy_url)
            try {
                startActivity(Intent(Intent.ACTION_VIEW, policyUrl.toUri()))
            } catch (_: Exception) {
                Toast.makeText(this, getString(R.string.unable_open_link), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
