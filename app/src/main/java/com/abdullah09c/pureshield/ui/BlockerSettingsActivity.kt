package com.abdullah09c.pureshield.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.abdullah09c.pureshield.databinding.ActivityBlockerSettingsBinding
import com.abdullah09c.pureshield.util.Prefs

class BlockerSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockerSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockerSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(com.abdullah09c.pureshield.R.string.title_blocker_settings)

        loadCurrentSettings()
        setupSaveButton()
    }

    private fun loadCurrentSettings() {
        binding.switchYoutube.isChecked = Prefs.isYouTubeBlocked(this)
        binding.switchFacebook.isChecked = Prefs.isFacebookBlocked(this)
        binding.switchFblite.isChecked = Prefs.isFBLiteBlocked(this)
        binding.switchInstagram.isChecked = Prefs.isInstagramBlocked(this)
        binding.switchTiktok.isChecked = Prefs.isTikTokBlocked(this)
        binding.switchBlockOnScroll.isChecked = Prefs.isBlockOnScroll(this)
        binding.etBlockMessage.setText(Prefs.getBlockMessage(this))
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            Prefs.setYouTubeBlocked(this, binding.switchYoutube.isChecked)
            Prefs.setFacebookBlocked(this, binding.switchFacebook.isChecked)
            Prefs.setFBLiteBlocked(this, binding.switchFblite.isChecked)
            Prefs.setInstagramBlocked(this, binding.switchInstagram.isChecked)
            Prefs.setTikTokBlocked(this, binding.switchTiktok.isChecked)
            Prefs.setBlockOnScroll(this, binding.switchBlockOnScroll.isChecked)

            val msg = binding.etBlockMessage.text.toString().trim()
            if (msg.isNotEmpty()) Prefs.setBlockMessage(this, msg)

            android.widget.Toast.makeText(this, "Settings saved", android.widget.Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
