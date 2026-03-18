package com.datainterview.app.ui.main

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
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

    private lateinit var statusText: TextView
    private lateinit var toggleButton: Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        activationManager = ActivationManager(this)

        statusText = findViewById(R.id.statusText)
        toggleButton = findViewById(R.id.toggleButton)
        scheduleButton = findViewById(R.id.scheduleButton)
        historyButton = findViewById(R.id.historyButton)
        permissionsButton = findViewById(R.id.permissionsButton)
        scheduleSection = findViewById(R.id.scheduleSection)
        scheduleStartText = findViewById(R.id.scheduleStartText)
        scheduleEndText = findViewById(R.id.scheduleEndText)
        scheduleStartButton = findViewById(R.id.scheduleStartButton)
        scheduleEndButton = findViewById(R.id.scheduleEndButton)
        scheduleConfirmButton = findViewById(R.id.scheduleConfirmButton)

        toggleButton.setOnClickListener { toggleActivation() }
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

    private fun updateUI() {
        scope.launch {
            val active = withContext(Dispatchers.IO) {
                activationManager.getActiveActivation()
            }
            if (active != null) {
                statusText.text = getString(R.string.status_active)
                statusText.setTextColor(getColorCompat(R.color.status_active))
                toggleButton.text = getString(R.string.action_stop)
            } else {
                statusText.text = getString(R.string.status_inactive)
                statusText.setTextColor(getColorCompat(R.color.status_inactive))
                toggleButton.text = getString(R.string.action_start)
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
                withContext(Dispatchers.IO) { activationManager.startActivation() }
                Toast.makeText(this@MainActivity, getString(R.string.status_active), Toast.LENGTH_SHORT).show()
            }
            updateUI()
        }
    }

    private fun toggleScheduleSection() {
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
                val formatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(cal.time)
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

    private fun getColorCompat(resId: Int): Int {
        return ContextCompat.getColor(this, resId)
    }
}
