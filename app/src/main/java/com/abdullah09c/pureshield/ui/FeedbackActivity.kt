package com.abdullah09c.pureshield.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.abdullah09c.pureshield.R
import com.abdullah09c.pureshield.databinding.ActivityFeedbackBinding

class FeedbackActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFeedbackBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.title_feedback)

        binding.btnRate.setOnClickListener {
            // Opens Play Store listing (update with real package after publish)
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=${packageName}")))
            } catch (_: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=${packageName}")))
            }
        }

        binding.btnBug.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.feedback_github_issues))))
        }

        binding.btnFeature.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.feedback_github_issues) + "?labels=enhancement")))
        }

        binding.btnEmail.setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse(getString(R.string.feedback_email_address))
                putExtra(Intent.EXTRA_SUBJECT, "PureShield Feedback")
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
