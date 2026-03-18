package com.datainterview.app.data.csv

import android.content.Context
import com.datainterview.app.data.entity.Event
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CsvGenerator(private val context: Context) {

    companion object {
        private const val DELIMITER = ";"
        private val HEADER = listOf(
            "type_interaction",
            "heure",
            "nom_app_widget",
            "heure_fermeture",
            "emplacement_widget"
        )

        /**
         * Writes CSV content to the given file. This static method allows
         * testing without an Android Context.
         */
        fun generateToFile(file: File, events: List<Event>) {
            FileWriter(file).use { writer ->
                writer.write(HEADER.joinToString(DELIMITER))
                writer.write("\n")
                for (event in events) {
                    val row = listOf(
                        event.interactionType,
                        event.time,
                        event.appOrWidgetName ?: "",
                        event.closeTime ?: "",
                        event.widgetLocation ?: ""
                    )
                    writer.write(row.joinToString(DELIMITER))
                    writer.write("\n")
                }
            }
        }
    }

    fun generate(events: List<Event>, activationId: Long): File {
        val dir = File(context.filesDir, "csv")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(dir, "data_interview_${activationId}_${timestamp}.csv")

        generateToFile(file, events)

        return file
    }
}
