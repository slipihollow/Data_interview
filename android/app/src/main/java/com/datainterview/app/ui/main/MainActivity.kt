package com.datainterview.app.ui.main

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.datainterview.app.R
import com.datainterview.app.activation.ActivationManager
import com.datainterview.app.ui.history.HistoryActivity
import com.datainterview.app.ui.permissions.PermissionsActivity
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var activationManager: ActivationManager
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var participantIdInput: TextInputEditText
    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
    private lateinit var capture1hButton: Button
    private lateinit var capture24hButton: Button
    private lateinit var scheduleButton: Button
    private lateinit var historyButton: Button
    private lateinit var permissionsButton: Button
    private lateinit var scheduleStartText: TextView
    private lateinit var scheduleEndText: TextView
    private lateinit var scheduleStartButton: Button
    private lateinit var scheduleEndButton: Button
    private lateinit var scheduleConfirmButton: Button
    private lateinit var scheduleSection: View

    private var scheduledStartTime: Long = 0
    private var scheduledEndTime: Long = 0

    private val prefs by lazy { getSharedPreferences("data_interview", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activationManager = ActivationManager(this)

        participantIdInput = findViewById(R.id.participantIdInput)
        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        capture1hButton = findViewById(R.id.capture1hButton)
        capture24hButton = findViewById(R.id.capture24hButton)
        scheduleButton = findViewById(R.id.scheduleButton)
        historyButton = findViewById(R.id.historyButton)
        permissionsButton = findViewById(R.id.permissionsButton)
        scheduleSection = findViewById(R.id.scheduleSection)
        scheduleStartText = findViewById(R.id.scheduleStartText)
        scheduleEndText = findViewById(R.id.scheduleEndText)
        scheduleStartButton = findViewById(R.id.scheduleStartButton)
        scheduleEndButton = findViewById(R.id.scheduleEndButton)
        scheduleConfirmButton = findViewById(R.id.scheduleConfirmButton)

        // Enforce 4-digit max via InputFilter as well
        participantIdInput.filters = arrayOf(InputFilter.LengthFilter(4))

        // Restore saved participant ID and lock state
        val savedId = prefs.getString("participant_id", null)
        if (!savedId.isNullOrBlank()) {
            participantIdInput.setText(savedId)
        }
        if (prefs.getBoolean("participant_id_locked", false) && !savedId.isNullOrBlank()) {
            lockParticipantId()
        }

        // Save participant ID on change and update button states
        participantIdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val id = s?.toString()?.trim() ?: ""
                prefs.edit().putString("participant_id", id).apply()
                updateActionButtonsEnabled()
            }
        })

        // When the field loses focus with a non-empty value that isn't locked yet, ask for confirmation
        participantIdInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus && !prefs.getBoolean("participant_id_locked", false)) {
                val id = getParticipantId()
                if (id.isNotEmpty()) {
                    showParticipantIdConfirmation(id)
                }
            }
        }

        // Long-press on the locked field to unlock
        participantIdInput.setOnLongClickListener {
            if (prefs.getBoolean("participant_id_locked", false)) {
                showUnlockConfirmation()
                true
            } else {
                false
            }
        }

        toggleButton.setOnClickListener { toggleActivation() }
        capture1hButton.setOnClickListener { startTimedCapture(1) }
        capture24hButton.setOnClickListener { startTimedCapture(24) }
        scheduleButton.setOnClickListener { toggleScheduleSection() }
        historyButton.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        permissionsButton.setOnClickListener { startActivity(Intent(this, PermissionsActivity::class.java)) }
        scheduleStartButton.setOnClickListener { pickDateTime(true) }
        scheduleEndButton.setOnClickListener { pickDateTime(false) }
        scheduleConfirmButton.setOnClickListener { confirmSchedule() }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun getParticipantId(): String = participantIdInput.text?.toString()?.trim() ?: ""

    private fun requireParticipantId(): Boolean {
        if (getParticipantId().isEmpty()) {
            Toast.makeText(this, getString(R.string.error_no_participant_id), Toast.LENGTH_SHORT).show()
            participantIdInput.requestFocus()
            return false
        }
        return true
    }

    private fun updateActionButtonsEnabled() {
        val hasId = getParticipantId().isNotEmpty()
        toggleButton.isEnabled = hasId
        capture1hButton.isEnabled = hasId
        capture24hButton.isEnabled = hasId
        scheduleButton.isEnabled = hasId
        scheduleConfirmButton.isEnabled = hasId
    }

    private fun updateUI() {
        updateActionButtonsEnabled()
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                activationManager.getActiveActivation()
            }
            if (active != null) {
                statusText.text = getString(R.string.status_active)
                statusText.setTextColor(getColorCompat(R.color.status_active))
                toggleButton.text = getString(R.string.action_stop)
                // Allow stopping even without ID
                toggleButton.isEnabled = true
            } else {
                statusText.text = getString(R.string.status_inactive)
                statusText.setTextColor(getColorCompat(R.color.status_inactive))
                toggleButton.text = getString(R.string.action_start)
                updateActionButtonsEnabled()
            }
        }
    }

    private fun toggleActivation() {
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                activationManager.getActiveActivation()
            }
            if (active != null) {
                withContext(Dispatchers.IO) { activationManager.stopActivation() }
                Toast.makeText(this@MainActivity, getString(R.string.status_inactive), Toast.LENGTH_SHORT).show()
            } else {
                if (!requireParticipantId()) return@launch
                withContext(Dispatchers.IO) { activationManager.startActivation() }
                Toast.makeText(this@MainActivity, getString(R.string.status_active), Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    private fun startTimedCapture(hours: Int) {
        if (!requireParticipantId()) return
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                activationManager.getActiveActivation()
            }
            if (active != null) {
                Toast.makeText(this@MainActivity, getString(R.string.status_active), Toast.LENGTH_SHORT).show()
                return@launch
            }
            val now = System.currentTimeMillis()
            val endTime = now + hours * 3600_000L
            withContext(Dispatchers.IO) {
                activationManager.startTimedActivation(endTime)
            }
            val msg = if (hours == 1) R.string.capture_started_1h else R.string.capture_started_24h
            Toast.makeText(this@MainActivity, getString(msg), Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }

    private fun toggleScheduleSection() {
        // Closing is always allowed; opening requires participant ID
        if (scheduleSection.visibility != View.VISIBLE && !requireParticipantId()) return
        scheduleSection.visibility =
            if (scheduleSection.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun pickDateTime(isStart: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                cal.set(year, month, day, hour, minute, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val millis = cal.timeInMillis
                val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRANCE).format(cal.time)
                if (isStart) {
                    scheduledStartTime = millis
                    scheduleStartText.text = formatted
                } else {
                    scheduledEndTime = millis
                    scheduleEndText.text = formatted
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun confirmSchedule() {
        if (!requireParticipantId()) return
        if (scheduledStartTime == 0L || scheduledEndTime == 0L) {
            Toast.makeText(this, getString(R.string.error_schedule_times), Toast.LENGTH_SHORT).show()
            return
        }
        if (scheduledEndTime <= scheduledStartTime) {
            Toast.makeText(this, getString(R.string.error_end_before_start), Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                activationManager.scheduleActivation(scheduledStartTime, scheduledEndTime)
            }
            Toast.makeText(this@MainActivity, getString(R.string.schedule_confirmed), Toast.LENGTH_SHORT).show()
            scheduleSection.visibility = View.GONE
        }
    }

    private fun showParticipantIdConfirmation(id: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.participant_id_confirm_title))
            .setMessage(getString(R.string.participant_id_confirm_message, id))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                prefs.edit().putBoolean("participant_id_locked", true).apply()
                lockParticipantId()
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                participantIdInput.text?.clear()
                participantIdInput.requestFocus()
            }
            .setCancelable(false)
            .show()
    }

    private fun lockParticipantId() {
        participantIdInput.isEnabled = false
        participantIdInput.alpha = 0.7f
    }

    private fun showUnlockConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.participant_id_unlock_title))
            .setMessage(getString(R.string.participant_id_unlock_message))
            .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                prefs.edit().putBoolean("participant_id_locked", false).apply()
                participantIdInput.isEnabled = true
                participantIdInput.alpha = 1.0f
                participantIdInput.requestFocus()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun getColorCompat(resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }
}
