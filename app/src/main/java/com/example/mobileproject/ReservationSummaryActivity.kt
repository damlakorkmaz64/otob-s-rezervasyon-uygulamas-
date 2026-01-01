package com.example.mobileproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ReservationSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_summary)

        val tvRoute = findViewById<TextView>(R.id.tvRoute)
        val tvDateTime = findViewById<TextView>(R.id.tvDateTime)
        val tvSeat = findViewById<TextView>(R.id.tvSeat)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        val seferId = intent.getIntExtra("sefer_id", -1)
        val from = intent.getStringExtra("from") ?: ""
        val to = intent.getStringExtra("to") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val time = intent.getStringExtra("time") ?: ""
        val seatNumber = intent.getIntExtra("seat_number", -1)

        tvRoute.text = "$from → $to"
        tvDateTime.text = "$date  $time"
        tvSeat.text = "Koltuk: $seatNumber"

        btnConfirm.setOnClickListener {
            // ✅ Success ekranına gerekli bilgileri yolla (Paylaş için)
            val successIntent = Intent(this, ReservationSuccessActivity::class.java).apply {
                putExtra("sefer_id", seferId)
                putExtra("from", from)
                putExtra("to", to)
                putExtra("date", date)
                putExtra("time", time)
                putExtra("seat_number", seatNumber)
            }

            startActivity(successIntent)
            finish()
        }
    }
}
