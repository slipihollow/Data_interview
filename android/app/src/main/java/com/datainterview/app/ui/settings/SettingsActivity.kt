package com.datainterview.app.ui.settings

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.datainterview.app.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        title = getString(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("data_interview", MODE_PRIVATE)

        val tokenEdit = findViewById<EditText>(R.id.tokenEdit)
        val chatIdEdit = findViewById<EditText>(R.id.chatIdEdit)
        val saveButton = findViewById<Button>(R.id.saveButton)

        // Load existing values
        tokenEdit.setText(prefs.getString("telegram_bot_token", ""))
        chatIdEdit.setText(prefs.getString("telegram_chat_id", ""))

        saveButton.setOnClickListener {
            val token = tokenEdit.text.toString().trim()
            val chatId = chatIdEdit.text.toString().trim()

            prefs.edit()
                .putString("telegram_bot_token", token)
                .putString("telegram_chat_id", chatId)
                .apply()

            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
