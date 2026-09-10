package com.monu.videoai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {

    private lateinit var chatContainer: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var serverLabel: TextView
    private lateinit var selectedLabel: TextView

    private val history = ArrayList<String>()
    private val prefs by lazy {
        getSharedPreferences("monu_chat_history", MODE_PRIVATE)
    }

    private var selectedFile: Uri? = null
    private var serverUrl = "http://127.0.0.1:8000"

    private val bg = Color.rgb(9, 10, 18)
    private val panel = Color.rgb(18, 20, 31)
    private val panel2 = Color.rgb(25, 27, 40)
    private val purple = Color.rgb(157, 91, 255)
    private val purpleDark = Color.rgb(91, 48, 145)
    private val green = Color.rgb(52, 211, 153)
    private val text = Color.rgb(242, 242, 247)
    private val muted = Color.rgb(160, 162, 175)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadHistory()
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 14, 12, 10)
            setBackgroundColor(panel)
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }

        val title = TextView(this).apply {
            text = "MONU"
            textSize = 25f
            setTextColor(purple)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val subtitle = TextView(this).apply {
            text = "Video AI • Master Command Center"
            textSize = 12f
            setTextColor(muted)
        }

        titleBox.addView(title)
        titleBox.addView(subtitle)

        val connection = TextView(this).apply {
            text = "● READY"
            textSize = 12f
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            background = rounded(panel2, 14)
        }

        val menu = TextView(this).apply {
            text = "⋮"
            textSize = 30f
            setTextColor(text)
            gravity = Gravity.CENTER
            setPadding(12, 0, 8, 0)
            isClickable = true
            setOnClickListener { showMainMenu() }
        }

        header.addView(titleBox)
        header.addView(connection)
        header.addView(menu)

        serverLabel = TextView(this).apply {
            text = "Server: $serverUrl"
            textSize = 12f
            setTextColor(muted)
            setPadding(18, 8, 18, 8)
            setBackgroundColor(panel)
        }

        chatContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 14, 14, 14)
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(
                chatContainer,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val chatWeight = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
            1f
        )

        val bottomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 12)
            setBackgroundColor(panel)
        }

        val selected = TextView(this).apply {
            text = ""
            textSize = 12f
            setTextColor(green)
            setPadding(6, 4, 6, 4)
            visibility = View.GONE
        }

        selectedLabel = TextView(this).apply {
            text = "No media selected"
            textSize = 12f
            setTextColor(muted)
            setPadding(8, 5, 8, 5)
        }

        val commandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val attach = actionButton("＋", purple) {
            chooseFile()
        }

        val voice = actionButton("🎙", green) {
            startVoice()
        }

        input = EditText(this).apply {
            hint = "Message MONU..."
            textSize = 16f
            setTextColor(text)
            setHintTextColor(muted)
            maxLines = 4
            setPadding(16, 12, 16, 12)
            background = rounded(panel2, 18)
        }

        val send = actionButton("➤", purple) {
            sendMessage()
        }

        commandRow.addView(
            attach,
            LinearLayout.LayoutParams(54, 54).apply {
                marginEnd = 7
            }
        )

        commandRow.addView(
            voice,
            LinearLayout.LayoutParams(54, 54).apply {
                marginEnd = 7
            }
        )

        commandRow.addView(
            input,
            LinearLayout.LayoutParams(0, 54, 1f).apply {
                marginEnd = 7
            }
        )

        commandRow.addView(
            send,
            LinearLayout.LayoutParams(58, 54)
        )

        status = TextView(this).apply {
            text = "MONU is ready"
            textSize = 12f
            setTextColor(muted)
            setPadding(6, 7, 6, 0)
        }

        bottomPanel.addView(selectedLabel)
        bottomPanel.addView(selected)
        bottomPanel.addView(commandRow)
        bottomPanel.addView(status)

        root.addView(header)
        root.addView(serverLabel)
        root.addView(scroll, chatWeight)
        root.addView(bottomPanel)

        setContentView(root)

        if (history.isEmpty()) {
            addMessage("Monu", "MONU is ready. Send a command or attach a video.")
        } else {
            addMessage("Monu", "Welcome back. Chat History is available from ⋮.")
        }
    }

    private fun actionButton(label: String, color: Int, click: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = rounded(color, 17)
            isClickable = true
            isFocusable = true
            setOnClickListener { click() }
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun addMessage(who: String, message: String) {
        val bubble = TextView(this).apply {
            text = "$who\n$message"
            textSize = 15f
            setTextColor(text)
            setPadding(16, 12, 16, 12)
            background = rounded(
                if (who == "Monu") panel2 else purpleDark,
                18
            )
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 10
        }

        chatContainer.addView(bubble, params)
    }

    private fun showMainMenu() {
        val popup = PopupMenu(this, findViewById<View>(android.R.id.content))

        popup.menu.add("📜 Chat History")
        popup.menu.add("🗑 Clear Current Chat")
        popup.menu.add("🌐 Server URL")

        popup.setOnMenuItemClickListener {
            when (it.title.toString()) {
                "📜 Chat History" -> showHistory()
                "🗑 Clear Current Chat" -> clearCurrentChat()
                "🌐 Server URL" -> editServer()
            }
            true
        }

        popup.show()
    }

    private fun showHistory() {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("MONU Chat History")
            .setItems(
                if (history.isEmpty()) {
                    arrayOf("No saved chats yet")
                } else {
                    history.toTypedArray()
                }
            ) { _, which ->
                if (history.isNotEmpty()) {
                    restoreHistory(history[which])
                }
            }
            .setNegativeButton("Close", null)
            .setPositiveButton("Clear History") { _, _ ->
                prefs.edit().remove("history").apply()
                history.clear()
            }
            .create()

        dialog.show()
    }

    private fun restoreHistory(item: String) {
        chatContainer.removeAllViews()
        addMessage("History", item)
        status.text = "Previous chat restored"
    }

    private fun clearCurrentChat() {
        chatContainer.removeAllViews()
        addMessage("Monu", "New chat started.")
        status.text = "Chat cleared"
    }

    private fun saveHistory(message: String) {
        history.add(0, message)

        while (history.size > 50) {
            history.removeAt(history.lastIndex)
        }

        prefs.edit()
            .putString("history", history.joinToString("\n---MONU---\n"))
            .apply()
    }

    private fun loadHistory() {
        val raw = prefs.getString("history", "") ?: ""
        if (raw.isNotEmpty()) {
            history.addAll(
                raw.split("\n---MONU---\n")
                    .filter { it.isNotBlank() }
            )
        }
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "video/*",
                    "image/*",
                    "application/pdf"
                )
            )
            addCategory(Intent.CATEGORY_OPENABLE)
        }

        startActivityForResult(intent, 1001)
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        val file = selectedFile

        if (message.isEmpty() && file == null) {
            status.text = "Type a command or attach a file"
            return
        }

        val shown = if (message.isEmpty()) {
            if (file != null) {
                "📎 ${getFileName(file)}"
            } else {
                "Media command"
            }
        } else {
            if (file != null) {
                "📎 ${getFileName(file)}\n$message"
            } else {
                message
            }
        }

        addMessage("You", shown)
        saveHistory(shown)

        input.setText("")
        selectedFile = null

        status.text = "Sending to MONU..."

        Thread {
            if (file != null) {
                val uploaded = uploadFile(file)

                if (uploaded != null) {
                    processMedia(uploaded, message)
                } else {
                    runOnUiThread {
                        status.text = "Upload failed"
                    }
                }
            } else {
                analyzeMessage(message)
            }
        }.start()
    }

    private fun uploadFile(uri: Uri): String? {
        return try {
            val name = getFileName(uri)
            val boundary = "MONU_${System.currentTimeMillis()}"

            val connection =
                URL("$serverUrl/upload").openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 120000
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val output = connection.outputStream
            val writer = output.bufferedWriter()

            writer.write("--$boundary\r\n")
            writer.write(
                "Content-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\n"
            )
            writer.write("Content-Type: application/octet-stream\r\n\r\n")
            writer.flush()

            val inputStream = contentResolver.openInputStream(uri)
                ?: return null

            inputStream.use { stream ->
                stream.copyTo(output)
            }

            output.flush()

            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
            writer.close()

            val code = connection.responseCode

            val response =
                if (code in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    ""
                }

            connection.disconnect()

            if (code !in 200..299) return null

            val marker = "\"filename\""
            val markerIndex = response.indexOf(marker)

            if (markerIndex < 0) return null

            val colon = response.indexOf(":", markerIndex)
            val firstQuote = response.indexOf('"', colon + 1)
            val secondQuote = response.indexOf('"', firstQuote + 1)

            if (firstQuote >= 0 && secondQuote > firstQuote) {
                response.substring(firstQuote + 1, secondQuote)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun processMedia(filename: String, message: String) {
        try {
            runOnUiThread {
                status.text = "MONU is processing media..."
            }

            val connection =
                URL("$serverUrl/media/process").openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15000
            connection.readTimeout = 300000
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val body =
                "{\"filename\":${jsonEscape(filename)},\"message\":${jsonEscape(message)}}"

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode

            val response =
                if (code in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText()
                        ?: "HTTP $code"
                }

            connection.disconnect()

            runOnUiThread {
                if (code in 200..299) {
                    addMessage("Monu", "🎬 Media processing complete\n$response")
                    status.text = "Processing completed"
                } else {
                    addMessage("Monu", "❌ Processing failed\n$response")
                    status.text = "Processing failed"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                status.text = "Processing error: ${e.message ?: "failed"}"
            }
        }
    }

    private fun analyzeMessage(message: String) {
        try {
            val encoded = URLEncoder.encode(message, "UTF-8")
            val connection =
                URL("$serverUrl/chat/analyze?message=$encoded")
                    .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val body = "{\"message\":${jsonEscape(message)}}"

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode

            val response =
                if (code in 200..299) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText()
                        ?: "HTTP $code"
                }

            connection.disconnect()

            runOnUiThread {
                if (code in 200..299) {
                    addMessage("Monu", response)
                    status.text = "MONU responded"
                } else {
                    addMessage("Monu", "Server error: $response")
                    status.text = "Server request failed"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                addMessage(
                    "Monu",
                    "Connection error: ${e.message ?: "unable to reach server"}"
                )
                status.text = "Server unavailable"
            }
        }
    }

    private fun editServer() {
        val field = EditText(this).apply {
            setText(serverUrl)
            setSingleLine(true)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("MONU Server URL")
            .setView(field)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val value = field.text.toString().trim().removeSuffix("/")
                if (value.isNotEmpty()) {
                    serverUrl = value
                    serverLabel.text = "Server: $serverUrl"
                    status.text = "Server URL updated"
                }
            }
            .show()
    }

    private fun jsonEscape(value: String): String {
        return "\"" +
                value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r") +
                "\""
    }


    private fun startVoice() {
        try {
            val intent = android.content.Intent(
                android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(
                    android.speech.RecognizerIntent.EXTRA_PROMPT,
                    "Speak to MONU"
                )
            }
            startActivityForResult(intent, 1002)
        } catch (e: Exception) {
            Toast.makeText(this, "MONU: Voice input unavailable: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val uri = data?.data

            if (uri == null) {
                selectedFile = null
                selectedLabel.text = "No media selected"
                status.text = "Selection cancelled"
                return
            }

            selectedFile = uri

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Some providers do not support persistable permissions.
            }

            val name = getFileName(uri)
            selectedLabel.text = "📎 $name"
            status.text = "Media selected — press ➤"
        }

        if (requestCode == 1002 && resultCode == RESULT_OK && data != null) {
            val results =
                data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )

            if (!results.isNullOrEmpty()) {
                input.setText(results[0])
                input.setSelection(input.length())
                status.text = "Voice command captured"
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = null

        contentResolver.query(
            uri,
            arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(
                    android.provider.OpenableColumns.DISPLAY_NAME
                )
                if (index >= 0) {
                    name = cursor.getString(index)
                }
            }
        }

        return name ?: (uri.lastPathSegment ?: "selected_media")
    }

}
