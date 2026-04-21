package com.followup.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.followup.R

class Seguridad : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seguridad)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
    }
}