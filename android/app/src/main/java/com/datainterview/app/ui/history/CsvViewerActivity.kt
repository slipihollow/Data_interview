package com.datainterview.app.ui.history

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.datainterview.app.R
import java.io.File

class CsvViewerActivity : AppCompatActivity() {

    private var csvFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_csv_viewer)

        title = getString(R.string.csv_viewer_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = intent.getStringExtra("csv_path") ?: run {
            finish()
            return
        }
        csvFile = File(path)
        if (!csvFile!!.exists()) {
            finish()
            return
        }

        val table = findViewById<TableLayout>(R.id.csvTable)
        val lines = csvFile!!.readLines()

        for ((index, line) in lines.withIndex()) {
            val row = TableRow(this).apply {
                setPadding(8, 4, 8, 4)
            }
            val cells = line.split(";")
            for (cell in cells) {
                val textView = TextView(this).apply {
                    text = cell
                    setPadding(16, 8, 16, 8)
                    if (index == 0) {
                        setTypeface(null, Typeface.BOLD)
                    }
                }
                row.addView(textView)
            }
            table.addView(row)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, getString(R.string.upload_telegram))
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            1 -> {
                shareCsv()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun shareCsv() {
        csvFile?.let { file ->
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, getString(R.string.upload_telegram)))
        }
    }
}
