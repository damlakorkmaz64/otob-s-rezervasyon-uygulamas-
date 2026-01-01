package com.example.mobileproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SeatSelectionActivity : AppCompatActivity() {

    // Emülatör için:
    private val BASE_URL = "http://10.0.2.2:8000"

    private lateinit var rvSeats: RecyclerView
    private lateinit var btnReserve: Button

    private lateinit var seats: MutableList<Seat>
    private lateinit var adapter: SeatAdapter

    private var seferId: Int = -1

    // ✅ Orientation için key'ler
    private val KEY_SELECTED_SEAT = "selected_seat"
    private val KEY_SEFER_ID = "sefer_id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seat_selection)

        // 🔹 MainActivity’den gelen sefer bilgileri
        val from = intent.getStringExtra("from")
        val to = intent.getStringExtra("to")
        val date = intent.getStringExtra("date")
        val time = intent.getStringExtra("time")

        // 🔹 kritik: sefer_id
        seferId = savedInstanceState?.getInt(KEY_SEFER_ID, -1)
            ?: intent.getIntExtra("sefer_id", -1)

        rvSeats = findViewById(R.id.rvSeats)
        btnReserve = findViewById(R.id.btnReserve)

        seats = (1..40).map { Seat(it) }.toMutableList()

        rvSeats.layoutManager = GridLayoutManager(this, 4)

        // px çevirici
        fun dp(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

        rvSeats.setHasFixedSize(true)

        // ❗ aynı decoration'ı iki kere eklememek için: (orientation'da tekrar eklenmesin)
        if (rvSeats.itemDecorationCount == 0) {
            rvSeats.addItemDecoration(
                SeatSpacingDecoration(
                    spanCount = 4,
                    spacingPx = dp(4),
                    aislePx = dp(12)
                )
            )
        }

        adapter = SeatAdapter(seats) { selectedSeat ->
            seats.forEach { seat ->
                seat.isSelected = (seat == selectedSeat)
            }
            adapter.notifyDataSetChanged()
        }

        rvSeats.adapter = adapter

        // ✅ 0) Orientation restore: seçili koltuğu geri yükle
        val restoredSeat = savedInstanceState?.getInt(KEY_SELECTED_SEAT, -1) ?: -1
        if (restoredSeat != -1) {
            seats.forEach { it.isSelected = (it.number == restoredSeat) }
            adapter.notifyDataSetChanged()
        }

        // ✅ 1) Ekran açılır açılmaz dolu koltukları çek
        if (seferId != -1) {
            fetchReservedSeats(seferId)
        } else {
            Toast.makeText(this, "Sefer ID bulunamadı.", Toast.LENGTH_LONG).show()
        }

        // ✅ 2) Rezervasyon yap
        btnReserve.setOnClickListener {
            val selectedSeat = seats.find { it.isSelected }
            if (selectedSeat == null) {
                Toast.makeText(this, "Lütfen koltuk seçin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            val userId = prefs.getInt("user_id", -1)

            if (userId == -1) {
                Toast.makeText(this, "User ID bulunamadı. Lütfen tekrar giriş yapın.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (seferId == -1) {
                Toast.makeText(this, "Sefer ID bulunamadı.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (selectedSeat.isReserved) {
                Toast.makeText(this, "Bu koltuk dolu.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnReserve.isEnabled = false
            btnReserve.text = "Rezerve ediliyor..."

            createReservation(
                userId = userId,
                seferId = seferId,
                seatNumber = selectedSeat.number,
                onSuccess = {
                    selectedSeat.isReserved = true
                    selectedSeat.isSelected = false
                    adapter.notifyDataSetChanged()

                    val summaryIntent = Intent(this, ReservationSummaryActivity::class.java)
                    summaryIntent.putExtra("from", from)
                    summaryIntent.putExtra("to", to)
                    summaryIntent.putExtra("date", date)
                    summaryIntent.putExtra("time", time)
                    summaryIntent.putExtra("seat_number", selectedSeat.number)
                    summaryIntent.putExtra("sefer_id", seferId)
                    startActivity(summaryIntent)
                    finish()
                },
                onFinally = {
                    btnReserve.isEnabled = true
                    btnReserve.text = "Rezervasyon Yap"
                }
            )
        }
    }

    // ✅ Orientation: state kaydet (seçili koltuk + sefer id)
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val selected = seats.firstOrNull { it.isSelected }?.number ?: -1
        outState.putInt(KEY_SELECTED_SEAT, selected)
        outState.putInt(KEY_SEFER_ID, seferId)
    }

    private fun fetchReservedSeats(seferId: Int) {
        thread {
            try {
                val url = URL("$BASE_URL/reservations/sefer/$seferId/seats")
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

                val obj = JSONObject(body)
                val arr = obj.getJSONArray("reserved_seats")

                val reservedSet = HashSet<Int>()
                for (i in 0 until arr.length()) reservedSet.add(arr.getInt(i))

                runOnUiThread {
                    seats.forEach { seat ->
                        seat.isReserved = reservedSet.contains(seat.number)
                        if (seat.isReserved) seat.isSelected = false
                    }
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Log.e("SEAT_FETCH", "Reserved seats fetch error", e)
                }
            }
        }
    }

    private fun createReservation(
        userId: Int,
        seferId: Int,
        seatNumber: Int,
        onSuccess: () -> Unit,
        onFinally: () -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/reservations")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                val json = JSONObject().apply {
                    put("user_id", userId)
                    put("sefer_id", seferId)
                    put("seat_number", seatNumber)
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
                        Toast.makeText(this, "Rezervasyon oluşturuldu ✅", Toast.LENGTH_SHORT).show()
                        onSuccess()
                    } else {
                        val msg = try {
                            JSONObject(body).optString("detail", body)
                        } catch (_: Exception) {
                            body
                        }
                        Toast.makeText(this, "Rezervasyon başarısız: $msg", Toast.LENGTH_LONG).show()
                        Log.e("RES_POST", "HTTP $code body=$body")

                        if (code == 409) {
                            fetchReservedSeats(seferId)
                        }
                    }
                    onFinally()
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Rezervasyon FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("RES_POST", "Exception", e)
                    onFinally()
                }
            }
        }
    }
}

