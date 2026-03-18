package com.datainterview.app.ui.permissions

import android.Manifest
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.datainterview.app.R
import com.datainterview.app.service.MediaNotificationListenerService

class PermissionsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val permissions = mutableListOf<PermissionItem>()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissions() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        title = getString(R.string.permissions_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.permissionsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        refreshPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissions()
    }

    private fun refreshPermissions() {
        permissions.clear()

        // 1. Usage Access (PACKAGE_USAGE_STATS)
        permissions.add(PermissionItem(
            name = getString(R.string.perm_usage_access),
            description = getString(R.string.perm_usage_access_desc),
            granted = isUsageAccessGranted(),
            action = { openUsageAccessSettings() }
        ))

        // 2. Notification Listener (for MediaSessionManager)
        permissions.add(PermissionItem(
            name = getString(R.string.perm_notification_access),
            description = getString(R.string.perm_notification_access_desc),
            granted = isNotificationListenerGranted(),
            action = { openNotificationListenerSettings() }
        ))

        // 3. POST_NOTIFICATIONS (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(PermissionItem(
                name = getString(R.string.perm_notifications),
                description = getString(R.string.perm_notifications_desc),
                granted = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED,
                action = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            ))
        }

        // 4. Battery Optimization (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions.add(PermissionItem(
                name = getString(R.string.perm_battery),
                description = getString(R.string.perm_battery_desc),
                granted = isBatteryOptimizationDisabled(),
                action = { requestBatteryOptimization() }
            ))
        }

        recyclerView.adapter = PermissionsAdapter(permissions)
    }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isNotificationListenerGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        val componentName = ComponentName(
            this, MediaNotificationListenerService::class.java
        ).flattenToString()
        return flat != null && flat.contains(componentName)
    }

    private fun isBatteryOptimizationDisabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openUsageAccessSettings() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    data class PermissionItem(
        val name: String,
        val description: String,
        val granted: Boolean,
        val action: () -> Unit
    )

    private class PermissionsAdapter(
        private val items: List<PermissionItem>
    ) : RecyclerView.Adapter<PermissionsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(R.id.permNameText)
            val descText: TextView = view.findViewById(R.id.permDescText)
            val statusText: TextView = view.findViewById(R.id.permStatusText)
            val actionButton: Button = view.findViewById(R.id.permActionButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_permission, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.nameText.text = item.name
            holder.descText.text = item.description
            if (item.granted) {
                holder.statusText.text = holder.itemView.context.getString(R.string.perm_granted)
                holder.statusText.setTextColor(0xFF4CAF50.toInt())
                holder.actionButton.visibility = View.GONE
            } else {
                holder.statusText.text = holder.itemView.context.getString(R.string.perm_not_granted)
                holder.statusText.setTextColor(0xFFF44336.toInt())
                holder.actionButton.visibility = View.VISIBLE
                holder.actionButton.text = holder.itemView.context.getString(R.string.perm_grant)
                holder.actionButton.setOnClickListener { item.action() }
            }
        }

        override fun getItemCount() = items.size
    }
}
