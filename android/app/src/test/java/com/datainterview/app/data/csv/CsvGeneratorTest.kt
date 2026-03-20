package com.datainterview.app.data.csv

import com.datainterview.app.data.entity.Event
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CsvGeneratorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `csv header is correct`() {
        val file = tempFolder.newFile("test.csv")
        val events = emptyList<Event>()
        CsvGenerator.generateToFile(file, events)

        val lines = file.readLines()
        assertEquals("type_interaction;heure;nom_app_widget;heure_fermeture;emplacement_widget", lines[0])
    }

    @Test
    fun `unlock event produces correct row`() {
        val file = tempFolder.newFile("test.csv")
        val events = listOf(
            Event(
                activationId = 1,
                interactionType = "deverrouillage",
                time = "14:30:00",
                appOrWidgetName = null,
                closeTime = null,
                widgetLocation = null
            )
        )
        CsvGenerator.generateToFile(file, events)

        val lines = file.readLines()
        assertEquals(2, lines.size)
        assertEquals("deverrouillage;14:30;;;", lines[1])
    }

    @Test
    fun `app event produces correct row`() {
        val file = tempFolder.newFile("test.csv")
        val events = listOf(
            Event(
                activationId = 1,
                interactionType = "application",
                time = "14:30:00",
                appOrWidgetName = "Spotify",
                closeTime = "14:45:00",
                widgetLocation = null
            )
        )
        CsvGenerator.generateToFile(file, events)

        val lines = file.readLines()
        assertEquals("application;14:30;Spotify;14:45;", lines[1])
    }

    @Test
    fun `widget event produces correct row`() {
        val file = tempFolder.newFile("test.csv")
        val events = listOf(
            Event(
                activationId = 1,
                interactionType = "widget",
                time = "14:30:00",
                appOrWidgetName = "Spotify",
                closeTime = null,
                widgetLocation = "ecran_verrouillage"
            )
        )
        CsvGenerator.generateToFile(file, events)

        val lines = file.readLines()
        assertEquals("widget;14:30;Spotify;;ecran_verrouillage", lines[1])
    }

    @Test
    fun `multiple events produce multiple rows`() {
        val file = tempFolder.newFile("test.csv")
        val events = listOf(
            Event(activationId = 1, interactionType = "deverrouillage", time = "14:30:00", appOrWidgetName = null, closeTime = null, widgetLocation = null),
            Event(activationId = 1, interactionType = "application", time = "14:30:00", appOrWidgetName = "Chrome", closeTime = "14:35:00", widgetLocation = null),
            Event(activationId = 1, interactionType = "widget", time = "14:36:00", appOrWidgetName = "Spotify", closeTime = null, widgetLocation = "ecran_verrouillage")
        )
        CsvGenerator.generateToFile(file, events)

        val lines = file.readLines()
        assertEquals(4, lines.size) // header + 3 events
    }
}
