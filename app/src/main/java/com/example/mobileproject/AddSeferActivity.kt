package com.example.mobileproject

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import kotlin.concurrent.thread

class AddSeferActivity : AppCompatActivity() {

    private val BASE_URL = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sefer)

        val etFrom = findViewById<EditText>(R.id.etFrom)
        val etTo = findViewById<EditText>(R.id.etTo)
        val etDate = findViewById<EditText>(R.id.etDate)
        val etTime = findViewById<EditText>(R.id.etTime)
        val btnSave = findViewById<Button>(R.id.btnSaveSefer)

        // ✅ Date picker
        etDate.setOnClickListener {
            openDatePicker(etDate)
        }

        // ✅ Time picker
        etTime.setOnClickListener {
            openTimePicker(etTime)
        }

        btnSave.setOnClickListener {
            val from = etFrom.text.toString().trim()
            val to = etTo.text.toString().trim()
            val date = etDate.text.toString().trim()
            val time = etTime.text.toString().trim()

            if (from.isEmpty() || to.isEmpty() || date.isEmpty() || time.isEmpty()) {
                Toast.makeText(this, "Boş alan bırakmayın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Kaydediliyor..."

            addSeferToBackend(
                from = from,
                to = to,
                date = date,
                time = time,
                onDone = {
                    btnSave.isEnabled = true
                    btnSave.text = "Seferi Kaydet"
                }
            )
        }
    }

    private fun openDatePicker(etDate: EditText) {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dp = DatePickerDialog(
            this,
            { _, y, m, d ->
                // GG.AA.YYYY
                val selectedDate = String.format("%02d.%02d.%04d", d, m + 1, y)
                etDate.setText(selectedDate)
            },
            year, month, day
        )

        // Bonus: geçmiş tarih seçilmesin (istersen kaldırabilirsin)
        dp.datePicker.minDate = System.currentTimeMillis() - 1000

        dp.show()
    }

    private fun openTimePicker(etTime: EditText) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)

        val tp = TimePickerDialog(
            this,
            { _, h, m ->
                // SS:DD
                val selectedTime = String.format("%02d:%02d", h, m)
                etTime.setText(selectedTime)
            },
            hour, minute,
            true // 24 saat formatı
        )

        tp.show()
    }

    private fun addSeferToBackend(
        from: String,
        to: String,
        date: String,
        time: String,
        onDone: () -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/admin/seferler")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                // Backend body: from_ alanı!
                val json = JSONObject().apply {
                    put("from_", from)
                    put("to", to)
                    put("date", date)
                    put("time", time)
                }

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().readText()

                conn.disconnect()

                runOnUiThread {
                    if (code in 200..299) {
                        Toast.makeText(this, "Sefer eklendi ✅", Toast.LENGTH_SHORT).show()
                        finish() // admin ekrana dön
                    } else {
                        Toast.makeText(this, "Hata: HTTP $code", Toast.LENGTH_LONG).show()
                        Log.e("ADD_SEFER", "HTTP $code body=$body")
                    }
                    onDone()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ADD_SEFER", "Exception", e)
                    onDone()
                }
            }
        }
    }
}


