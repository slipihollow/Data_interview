package com.datainterview.app.ui.history

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datainterview.app.R
import com.datainterview.app.activation.ActivationManager
import com.datainterview.app.data.entity.Activation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        title = getString(R.string.history_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.activationsRecyclerView)
        emptyText = findViewById(R.id.emptyText)
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() {
        super.onResume()
        loadActivations()
    }

    private fun loadActivations() {
        scope.launch {
            val activations = withContext(Dispatchers.IO) {
                ActivationManager(this@HistoryActivity).getAllActivations()
            }
            if (activations.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = ActivationAdapter(activations) { activation ->
                    activation.csvFilePath?.let { path ->
                        val intent = Intent(this@HistoryActivity, CsvViewerActivity::class.java)
                        intent.putExtra("csv_path", path)
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private class ActivationAdapter(
        private val activations: List<Activation>,
        private val onClick: (Activation) -> Unit
    ) : RecyclerView.Adapter<ActivationAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.dateText)
            val durationText: TextView = view.findViewById(R.id.durationText)
            val eventCountText: TextView = view.findViewById(R.id.eventCountText)
            val statusText: TextView = view.findViewById(R.id.uploadStatusText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_activation, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val activation = activations[position]
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE)

            val startStr = dateFormat.format(Date(activation.startTime))
            val endStr = activation.endTime?.let { dateFormat.format(Date(it)) } ?: "..."
            holder.dateText.text = "$startStr \u2192 $endStr"

            val durationMs = (activation.endTime ?: System.currentTimeMillis()) - activation.startTime
            val hours = durationMs / 3600000
            val minutes = (durationMs % 3600000) / 60000
            holder.durationText.text = "${hours}h ${minutes}min"

            holder.eventCountText.text = "${activation.eventCount} \u00e9v\u00e9nements"

            holder.statusText.text = when (activation.uploadStatus) {
                "success" -> "\u2713 Envoy\u00e9"
                "failed" -> "\u2717 \u00c9chec"
                "pending" -> "\u231b En attente"
                else -> activation.status
            }

            holder.itemView.setOnClickListener { onClick(activation) }
        }

        override fun getItemCount() = activations.size
    }
}
