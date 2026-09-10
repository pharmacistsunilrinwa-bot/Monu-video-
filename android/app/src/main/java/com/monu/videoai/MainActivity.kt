package com.monu.videoai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "MONU Video AI"
            textSize = 28f
            setTextColor(Color.BLACK)
        }

        val greeting = TextView(this).apply {
            text = "What can I help with, Sunil?"
            textSize = 22f
            setTextColor(Color.DKGRAY)
            setPadding(0, 32, 0, 24)
        }

        val modelLabel = TextView(this).apply {
            text = "Model"
            textSize = 16f
            setTextColor(Color.DKGRAY)
        }

        val models = arrayOf(
            "gemini-3.1-flash-lite",
            "gemini-3-flash-preview",
            "gemini-3.5-flash",
            "gemma-4-31b-it",
            "gemma-4-26b-a4b-it"
        )

        val modelSpinner = Spinner(this)
        modelSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            models
        )

        root.addView(title)
        root.addView(greeting)
        root.addView(modelLabel)
        root.addView(
            modelSpinner,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }
}
