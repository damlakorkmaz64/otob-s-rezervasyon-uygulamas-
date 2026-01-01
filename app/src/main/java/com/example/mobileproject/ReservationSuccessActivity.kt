package com.example.mobileproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReservationSuccessActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_success)

        val btnGoHome = findViewById<Button>(R.id.btnGoHome)
        val btnShare = findViewById<Button>(R.id.btnShare)

        // Bu bilgileri Summary ekranından buraya intent ile göndermek en temizi.
        // Eğer şimdilik yoksa "Bilinmiyor" yazar.
        val from = intent.getStringExtra("from") ?: "Bilinmiyor"
        val to = intent.getStringExtra("to") ?: "Bilinmiyor"
        val date = intent.getStringExtra("date") ?: "-"
        val time = intent.getStringExtra("time") ?: "-"
        val seat = intent.getIntExtra("seat_number", -1)

        val shareText = buildString {
            append("✅ Rezervasyon Bilgileri\n")
            append("$from → $to\n")
            append("Tarih/Saat: $date $time\n")
            if (seat != -1) append("Koltuk: $seat\n")
            append("\nİyi yolculuklar! ✨")
        }

        btnShare.setOnClickListener {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Rezervasyon Bilgileri")
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                startActivity(Intent.createChooser(shareIntent, "Paylaş"))
            } catch (e: Exception) {
                Toast.makeText(this, "Paylaşım açılamadı: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}

