package com.example.audiotest

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class MainActivity : AppCompatActivity() {
    private val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
    private val PERMISSION_REQUEST_CODE = 123
    private lateinit var buttonStart: Button
    private lateinit var buttonStop: Button

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        buttonStart = findViewById(R.id.buttonStart)
        buttonStop = findViewById(R.id.buttonStop)
        val audioManipulator = AudioManipulator()
        if (!checkPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            buttonStart.visibility = View.VISIBLE
        }
        buttonStart.setOnClickListener {
            audioManipulator.start()
            buttonStart.visibility = View.GONE
            buttonStop.visibility = View.VISIBLE
        }

        buttonStop.setOnClickListener {
            audioManipulator.stop()
            buttonStop.visibility = View.GONE
            buttonStart.visibility = View.VISIBLE
        }
    }

    private fun checkPermission() : Boolean {
        return ContextCompat.checkSelfPermission(this, PERMISSION_RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_LONG).show()
                buttonStart.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Permission denied, aborting...", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}