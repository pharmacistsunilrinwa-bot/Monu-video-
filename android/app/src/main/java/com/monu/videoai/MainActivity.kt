package com.monu.videoai

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.FileProvider
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

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
    private var cameraUri: Uri? = null
    private var serverUrl = prefs.getString(
        "server_url",
        "http://127.0.0.1:8000"
    ) ?: "http://127.0.0.1:8000"

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
            setTextColor(this@MainActivity.text)
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

        var attach: TextView? = null
        attach = actionButton("＋", purple) {
            showMediaMenu(attach!!)
        }

        val voice = actionButton("🎙", green) {
            startVoice()
        }

        input = EditText(this).apply {
            hint = "Message MONU..."
            textSize = 16f
            setTextColor(this@MainActivity.text)
            setHintTextColor(muted)
            maxLines = 4
            setPadding(16, 12, 16, 12)
            background = rounded(panel2, 18)
        }

        val send = actionButton("➤", purple) {
            sendMessage()
        }

        commandRow.addView(
            attach!!,
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
            setTextColor(this@MainActivity.text)
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

    private fun showMediaMenu(anchor: View) {
        val options = arrayOf(
            "📷 Camera",
            "🖼 Photo",
            "🎥 Video",
            "📄 File"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Add to MONU")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openCamera()
                    1 -> chooseMedia("image/*")
                    2 -> chooseMedia("video/*")
                    3 -> chooseMedia("*/*")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun chooseMedia(type: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            this.type = type
            addCategory(Intent.CATEGORY_OPENABLE)

            if (type == "*/*") {
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "image/*",
                        "video/*",
                        "application/pdf",
                        "text/*"
                    )
                )
            }
        }

        startActivityForResult(intent, 1001)
    }

    private fun openCamera() {
        try {
            val photoFile = java.io.File.createTempFile(
                "MONU_CAMERA_",
                ".jpg",
                cacheDir
            )

            cameraUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            startActivityForResult(intent, 1003)
        } catch (e: Exception) {
            cameraUri = null
            Toast.makeText(
                this,
                "MONU: Camera unavailable: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun sendMessage() {
        val message = input.text.toString().trim()
        val file = selectedFile

        if (message.isEmpty() && file == null) {
            status.text = "Type a command or attach a file"
            return
        }

        input.setText("")

        if (file == null) {
            addMessage("You", message)
            saveHistory(message)
            status.text = "Sending to MONU..."

            Thread {
                analyzeMessage(message)
            }.start()

            return
        }

        status.text = "Uploading media..."

        Thread {
            val uploaded = uploadFile(file)

            if (uploaded == null) {
                runOnUiThread {
                    status.text = "Upload failed — media was NOT sent"
                    Toast.makeText(
                        this,
                        "MONU: Upload failed. The media was not sent.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@Thread
            }

            val name = getFileName(file)
            val shown = if (message.isEmpty()) {
                "📎 $name"
            } else {
                "📎 $name\n$message"
            }

            runOnUiThread {
                addMessage("You", shown)
                saveHistory(shown)
                selectedFile = null
                selectedLabel.text = "No media selected"
                status.text = "Media uploaded — processing..."
            }

            processMedia(uploaded, message)
        }.start()
    }

    private fun uploadFile(uri: Uri): String? {
        var connection: HttpURLConnection? = null

        return try {
            val name = getFileName(uri)
            val mime = contentResolver.getType(uri)
                ?: "application/octet-stream"

            val boundary = "----MONU${System.currentTimeMillis()}"
            val url = URL("$serverUrl/upload")

            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.useCaches = false
            connection.connectTimeout = 15000
            connection.readTimeout = 120000

            connection.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            connection.outputStream.use { output ->
                fun writeText(value: String) {
                    output.write(value.toByteArray(Charsets.UTF_8))
                }

                writeText("--$boundary\r\n")
                writeText(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"$name\"\r\n"
                )
                writeText("Content-Type: $mime\r\n")
                writeText("Content-Transfer-Encoding: binary\r\n\r\n")

                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Unable to read selected media")

                inputStream.use { stream ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = stream.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                    }
                }

                writeText("\r\n--$boundary--\r\n")
                output.flush()
            }

            val code = connection.responseCode

            val response =
                if (code in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: "HTTP $code"
                }

            if (code !in 200..299) {
                return null
            }

            val json = JSONObject(response)
            val filename = json.optString("filename", "")

            if (filename.isBlank()) {
                null
            } else {
                filename
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun processMedia(filename: String, message: String) {
        Thread {
            var connection: HttpURLConnection? = null

            try {
                runOnUiThread {
                    status.text = "MONU is processing video..."
                }

                connection =
                    URL("$serverUrl/media/process").openConnection()
                        as HttpURLConnection

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
                        connection.inputStream
                            .bufferedReader()
                            .use { it.readText() }
                    } else {
                        connection.errorStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: "HTTP $code"
                    }

                if (code !in 200..299) {
                    runOnUiThread {
                        addMessage(
                            "Monu",
                            "❌ Video processing failed\n$response"
                        )
                        status.text = "Processing failed"
                    }
                    return@Thread
                }

                val json = JSONObject(response)

                val ok = json.optBoolean("ok", false)
                val outputPath = json.optString("output", "")
                val jobId = json.optString("job_id", "")

                if (!ok || outputPath.isBlank()) {
                    runOnUiThread {
                        addMessage(
                            "Monu",
                            "❌ Server did not return a verified cartoon video.\n$response"
                        )
                        status.text = "No verified output"
                    }
                    return@Thread
                }

                val outputName = java.io.File(outputPath).name

                if (outputName.isBlank()) {
                    runOnUiThread {
                        status.text = "Invalid processed filename"
                    }
                    return@Thread
                }

                runOnUiThread {
                    status.text = "Cartoon video verified — downloading..."
                }

                val savedUri = downloadProcessedVideo(
                    outputName,
                    jobId
                )

                if (savedUri == null) {
                    runOnUiThread {
                        addMessage(
                            "Monu",
                            "⚠️ Cartoon video was created on the server, but the real video download failed.\nOutput: $outputName"
                        )
                        status.text = "Cartoon created — download failed"
                    }
                    return@Thread
                }

                runOnUiThread {
                    addDownloadMessage(
                        "🎬 Cartoon video ready",
                        savedUri,
                        outputName
                    )
                    status.text = "Cartoon video downloaded successfully"
                }

            } catch (e: Exception) {
                runOnUiThread {
                    addMessage(
                        "Monu",
                        "❌ Processing/download error: ${e.message ?: "failed"}"
                    )
                    status.text = "Processing error"
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun downloadProcessedVideo(
        filename: String,
        jobId: String
    ): Uri? {
        var connection: HttpURLConnection? = null

        return try {
            connection =
                URL(
                    "$serverUrl/media/download/${URLEncoder.encode(filename, "UTF-8")}"
                ).openConnection() as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 300000

            val code = connection.responseCode

            if (code !in 200..299) {
                return null
            }

            val contentType =
                connection.contentType ?: "video/mp4"

            val resolver = contentResolver

            val values = android.content.ContentValues().apply {
                put(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    filename
                )
                put(
                    MediaStore.MediaColumns.MIME_TYPE,
                    contentType
                )
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    "Download/MONU"
                )
                put(
                    MediaStore.MediaColumns.IS_PENDING,
                    1
                )
            }

            val collection =
                MediaStore.Files.getContentUri("external")

            val uri = resolver.insert(
                collection,
                values
            ) ?: return null

            try {
                resolver.openOutputStream(uri)?.use { output ->
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(64 * 1024)

                        while (true) {
                            val count = input.read(buffer)

                            if (count <= 0) {
                                break
                            }

                            output.write(
                                buffer,
                                0,
                                count
                            )
                        }

                        output.flush()
                    }
                } ?: throw IllegalStateException(
                    "Cannot open destination"
                )

                val downloadedBytes =
                    resolver.openAssetFileDescriptor(
                        uri,
                        "r"
                    )?.use { it.length }
                        ?: -1L

                if (downloadedBytes <= 0L) {
                    resolver.delete(uri, null, null)
                    return null
                }

                val completed =
                    android.content.ContentValues().apply {
                        put(
                            MediaStore.MediaColumns.IS_PENDING,
                            0
                        )
                    }

                resolver.update(
                    uri,
                    completed,
                    null,
                    null
                )

                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }

        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun addDownloadMessage(
        title: String,
        uri: Uri,
        filename: String
    ) {
        addMessage(
            "Monu",
            "$title\n$filename\n\n✅ Real video saved to Download/MONU"
        )

        val downloadButton = TextView(this).apply {
            text = "⬇️ Open / Download Cartoon Video"
            textSize = 15f
            setPadding(28, 20, 28, 20)
            setTextColor(this@MainActivity.text)

            setOnClickListener {
                try {
                    val intent =
                        Intent(
                            Intent.ACTION_VIEW,
                            uri
                        ).apply {
                            setDataAndType(
                                uri,
                                "video/mp4"
                            )
                            addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }

                    startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(
                        this@MainActivity,
                        "MONU: No video app available to open this file.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        chatContainer.addView(downloadButton)
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
                    prefs.edit()
                        .putString("server_url", serverUrl)
                        .apply()
                    serverLabel.text = "Server: $serverUrl"
                    status.text = "Server URL saved"
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

        if (requestCode == 1001) {
            if (resultCode != RESULT_OK || data?.data == null) {
                selectedFile = null
                selectedLabel.text = "No media selected"
                status.text = "Media selection cancelled"
                return
            }

            val uri = data.data!!

            selectedFile = uri

            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Provider does not support persistable permission.
            }

            val name = getFileName(uri)
            val mime = contentResolver.getType(uri) ?: "unknown"

            selectedLabel.text = "📎 $name"
            status.text = "Media selected ($mime) — press ➤"
            return
        }

        if (requestCode == 1002 &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val results = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
            )

            if (!results.isNullOrEmpty()) {
                input.setText(results[0])
                input.setSelection(input.length())
                status.text = "Voice command captured"
            }
            return
        }

        if (requestCode == 1003) {
            if (resultCode != RESULT_OK || cameraUri == null) {
                cameraUri = null
                selectedFile = null
                selectedLabel.text = "No media selected"
                status.text = "Camera cancelled"
                return
            }

            val uri = cameraUri
            if (uri == null) {
                status.text = "Camera URI unavailable"
                return
            }

            try {
                val name = getFileName(uri)

                selectedFile = uri
                selectedLabel.text = "📷 $name"
                status.text = "Camera photo ready — press ➤"
            } catch (e: Exception) {
                cameraUri = null
                selectedFile = null
                selectedLabel.text = "No media selected"
                status.text = "Camera result failed"
            }
            return
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
