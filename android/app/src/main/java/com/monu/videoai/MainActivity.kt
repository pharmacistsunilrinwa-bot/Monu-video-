package com.monu.videoai

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity() {

    private lateinit var messages: LinearLayout
    private lateinit var input: EditText
    private lateinit var status: TextView
    private var selectedUri: Uri? = null
    private var serverUrl = "http://127.0.0.1:8000"

    private val pickFile = 1001
    private val voiceInput = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 24, 20, 12)
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "MONU Video AI"
            textSize = 24f
            setTextColor(0xFF111111.toInt())
        }

        val menu = Button(this).apply {
            text = "⋮"
            textSize = 24f
            setOnClickListener { showMenu(this) }
        }

        top.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(menu)

        status = TextView(this).apply {
            text = "Ready"
            textSize = 13f
            setPadding(0, 4, 0, 12)
        }

        val scroll = ScrollView(this)
        messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(messages)

        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val plus = Button(this).apply {
            text = "+"
            textSize = 22f
            setOnClickListener { chooseFile() }
        }

        input = EditText(this).apply {
            hint = "Message MONU..."
            minLines = 1
            maxLines = 4
        }

        val voice = Button(this).apply {
            text = "🎤"
            setOnClickListener { startVoice() }
        }

        val send = Button(this).apply {
            text = "Send"
            setOnClickListener { sendMessage() }
        }

        row.addView(plus, LinearLayout.LayoutParams(52, -2))
        row.addView(input, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(voice, LinearLayout.LayoutParams(58, -2))
        row.addView(send, LinearLayout.LayoutParams(80, -2))

        root.addView(top)
        root.addView(status)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(row)

        setContentView(root)
        addMessage("Monu", "नमस्ते! मैं तैयार हूँ। संदेश भेजें या + से file चुनें।")
    }

    private fun addMessage(who: String, text: String) {
        val tv = TextView(this).apply {
            this.text = "$who: $text"
            textSize = 16f
            setPadding(8, 12, 8, 12)
        }
        messages.addView(tv)
    }

    private fun chooseFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, pickFile)
    }

    private fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        try {
            startActivityForResult(intent, voiceInput)
        } catch (e: Exception) {
            status.text = "Voice unavailable"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) return

        if (requestCode == pickFile) {
            selectedUri = data?.data
            selectedUri?.let {
                val name = getFileName(it)
                status.text = "Selected: $name"
                addMessage("You", "📎 $name")
            }
        }

        if (requestCode == voiceInput) {
            val result = data?.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )
            if (!result.isNullOrEmpty()) {
                input.setText(result[0])
                input.setSelection(input.text.length)
            }
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        val file = selectedUri

        if (message.isEmpty() && file == null) {
            status.text = "Enter a message or select a file"
            return
        }

        if (message.isNotEmpty()) addMessage("You", message)
        input.text.clear()

        thread {
            var uploadedName: String? = null

            if (file != null) {
                uploadedName = uploadFile(file)
            }

            if (uploadedName != null) {
                processMedia(uploadedName, message)
            }

            if (message.isNotEmpty() && file == null) {
                analyzeMessage(message)
            }

            runOnUiThread {
                selectedUri = null
            }
        }
    }

    private fun analyzeMessage(message: String) {
        try {
            val url = URL("$serverUrl/chat/analyze")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 90000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val body = """{"message":${jsonEscape(message)}}"""
            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val response = connection.inputStream.bufferedReader().readText()

            runOnUiThread {
                addMessage("Monu", response)
                status.text = "Server response received"
            }

            connection.disconnect()
        } catch (e: Exception) {
            runOnUiThread {
                status.text = "Server error: ${e.message ?: "connection failed"}"
            }
        }
    }

    private fun uploadFile(uri: Uri): String? {
        try {
            val name = getFileName(uri)
            val boundary = "MONU_${System.currentTimeMillis()}"

            val connection =
                URL("$serverUrl/upload").openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 120000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            val output = DataOutputStream(connection.outputStream)

            output.writeBytes("--$boundary\r\n")
            output.writeBytes(
                "Content-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\n"
            )
            output.writeBytes("Content-Type: application/octet-stream\r\n\r\n")

            contentResolver.openInputStream(uri)?.use { inputStream ->
                val buffer = ByteArray(8192)
                var count: Int
                while (inputStream.read(buffer).also { count = it } != -1) {
                    output.write(buffer, 0, count)
                }
            }

            output.writeBytes("\r\n--$boundary--\r\n")
            output.flush()
            output.close()

            val code = connection.responseCode

            val response = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                ""
            }

            connection.disconnect()

            if (code in 200..299) {
                val marker = "\"filename\""
                val markerIndex = response.indexOf(marker)
                val uploadedName = if (markerIndex >= 0) {
                    val colon = response.indexOf(":", markerIndex)
                    val firstQuote = response.indexOf('"', colon + 1)
                    val secondQuote = response.indexOf('"', firstQuote + 1)
                    if (firstQuote >= 0 && secondQuote > firstQuote) {
                        response.substring(firstQuote + 1, secondQuote)
                    } else {
                        null
                    }
                } else {
                    null
                }

                runOnUiThread {
                    addMessage("Monu", "✅ File uploaded: $name")
                    status.text = "Upload successful"
                }

                return uploadedName
            }

            runOnUiThread {
                status.text = "Upload failed: HTTP $code"
            }

            return null
        } catch (e: Exception) {
            runOnUiThread {
                status.text = "Upload error: ${e.message ?: "failed"}"
            }
            return null
        }
    }

    private fun processMedia(filename: String, message: String) {
        try {
            runOnUiThread {
                status.text = "Processing media..."
            }

            val url = URL("$serverUrl/media/process")
            val connection = url.openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 10000
            connection.readTimeout = 300000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )

            val body =
                """{"filename":${jsonEscape(filename)},"message":${jsonEscape(message)}}"""

            connection.outputStream.use {
                it.write(body.toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val response = if (code in 200..299) {
                connection.inputStream.bufferedReader().readText()
            } else {
                connection.errorStream?.bufferedReader()?.readText()
                    ?: "HTTP $code"
            }

            connection.disconnect()

            runOnUiThread {
                if (code in 200..299) {
                    addMessage("Monu", "🎬 Processing result: $response")
                    status.text = "Media processing completed"
                } else {
                    addMessage("Monu", "❌ Processing failed: $response")
                    status.text = "Media processing failed"
                }
            }
        } catch (e: Exception) {
            runOnUiThread {
                status.text =
                    "Processing error: ${e.message ?: "failed"}"
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "selected_file"

        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) name = cursor.getString(index)
            }
        }

        return name
    }

    private fun jsonEscape(value: String): String {
        return "\"" +
            value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r") +
            "\""
    }

    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add("Server: $serverUrl")
            menu.add("Clear chat")
            setOnMenuItemClickListener {
                when {
                    it.title == "Clear chat" -> {
                        messages.removeAllViews()
                        addMessage("Monu", "Chat cleared.")
                        true
                    }
                    it.title.toString().startsWith("Server:") -> {
                        editServerUrl()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun editServerUrl() {
        val field = EditText(this).apply {
            setText(serverUrl)
            selectAll()
        }

        AlertDialogBuilder(field)
    }

    private fun AlertDialogBuilder(field: EditText) {
        android.app.AlertDialog.Builder(this)
            .setTitle("MONU Server URL")
            .setMessage("Example: http://127.0.0.1:8000")
            .setView(field)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                serverUrl = field.text.toString().trim().removeSuffix("/")
                status.text = "Server: $serverUrl"
            }
            .show()
    }
}
