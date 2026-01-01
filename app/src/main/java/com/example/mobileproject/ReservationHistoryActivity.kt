package com.example.mobileproject

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ReservationHistoryActivity : AppCompatActivity() {

    private lateinit var rvReservationHistory: RecyclerView
    private lateinit var adapter: ReservationHistoryAdapter

    private val BASE_URL = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_history3)

        rvReservationHistory = findViewById(R.id.rvReservationHistory)
        rvReservationHistory.layoutManager = LinearLayoutManager(this)

        adapter = ReservationHistoryAdapter(mutableListOf())
        rvReservationHistory.adapter = adapter

        fetchReservations()
    }

    private fun fetchReservations() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userId = prefs.getInt("user_id", -1)

        if (userId == -1) {
            Toast.makeText(this, "User ID yok. Tekrar giriş yap.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        thread {
            try {
                val url = URL("$BASE_URL/reservations/user/$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().readText()

                conn.disconnect()

                if (code !in 200..299) throw RuntimeException("HTTP $code: $body")

                val list = parseReservationOut(body)

                runOnUiThread {
                    rvReservationHistory.adapter = ReservationHistoryAdapter(list.toMutableList())
                    Toast.makeText(this, "Rezervasyon sayısı: ${list.size}", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "History FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("RES_HISTORY", "Exception", e)
                }
            }
        }
    }

    private fun parseReservationOut(json: String): List<ReservationOut> {
        val arr = JSONArray(json)
        val list = mutableListOf<ReservationOut>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ReservationOut(
                    id = o.getInt("id"),
                    user_id = o.getInt("user_id"),
                    sefer_id = o.getInt("sefer_id"),
                    seat_number = o.getInt("seat_number"),
                    created_at = o.getString("created_at")
                )
            )
        }
        return list
    }
}


private fun parseReservationOut(json: String): List<ReservationOut> {
        val arr = JSONArray(json)
        val list = mutableListOf<ReservationOut>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                ReservationOut(
                    id = o.getInt("id"),
                    user_id = o.getInt("user_id"),
                    sefer_id = o.getInt("sefer_id"),
                    seat_number = o.getInt("seat_number"),
                    created_at = o.getString("created_at")
                )
            )
        }
        return list
    }





