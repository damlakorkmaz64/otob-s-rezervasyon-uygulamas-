package com.example.mobileproject

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val BASE_URL = "http://10.0.2.2:8000"

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SeferAdapter

    private var allSeferler: List<Sefer> = emptyList()

    private lateinit var etSearchFrom: EditText
    private lateinit var etSearchTo: EditText
    private lateinit var btnSearch: Button

    // ✅ Orientation key'leri
    private val KEY_FROM = "search_from"
    private val KEY_TO = "search_to"
    private val KEY_FILTER_ACTIVE = "filter_active"

    // ✅ Filtre açık mı?
    private var filterActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        recyclerView = findViewById(R.id.rvSeferler)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnHistory = findViewById<Button>(R.id.btnHistory)

        etSearchFrom = findViewById(R.id.etSearchFrom)
        etSearchTo = findViewById(R.id.etSearchTo)
        btnSearch = findViewById(R.id.btnSearch)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = SeferAdapter(mutableListOf()) { sefer ->
            val intent = Intent(this, SeatSelectionActivity::class.java)
            intent.putExtra("from", sefer.from)
            intent.putExtra("to", sefer.to)
            intent.putExtra("date", sefer.date)
            intent.putExtra("time", sefer.time)
            intent.putExtra("sefer_id", sefer.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // ✅ Restore (orientation)
        savedInstanceState?.let {
            etSearchFrom.setText(it.getString(KEY_FROM, ""))
            etSearchTo.setText(it.getString(KEY_TO, ""))
            filterActive = it.getBoolean(KEY_FILTER_ACTIVE, false)
        }

        btnSearch.setOnClickListener {
            applyFilter()
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, ReservationHistoryActivity::class.java))
        }

        btnLogout.setOnClickListener {
            prefs.edit().clear().apply()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        testBackendConnection()
        fetchSeferlerAndBind()
    }

    override fun onResume() {
        super.onResume()
        fetchSeferlerAndBind()
    }

    // ✅ Orientation: state kaydet
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FROM, etSearchFrom.text.toString())
        outState.putString(KEY_TO, etSearchTo.text.toString())
        outState.putBoolean(KEY_FILTER_ACTIVE, filterActive)
    }

    // ✅ Filtreyi tek yerden uygula
    private fun applyFilter() {
        val qFrom = etSearchFrom.text.toString().trim().lowercase()
        val qTo = etSearchTo.text.toString().trim().lowercase()

        filterActive = qFrom.isNotEmpty() || qTo.isNotEmpty()

        val filtered = allSeferler.filter { s ->
            val okFrom = qFrom.isEmpty() || s.from.lowercase().contains(qFrom)
            val okTo = qTo.isEmpty() || s.to.lowercase().contains(qTo)
            okFrom && okTo
        }

        adapter.updateList(filtered.toMutableList())
        Toast.makeText(this, "Bulunan sefer: ${filtered.size}", Toast.LENGTH_SHORT).show()
    }

    private fun testBackendConnection() {
        thread {
            try {
                val url = URL("$BASE_URL/")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader()
                    .readText()

                runOnUiThread {
                    Log.d("API_TEST", "code=$code body=$body")
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Backend FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_TEST", "Backend bağlantı hatası", e)
                }
            }
        }
    }

    private fun fetchSeferlerAndBind() {
        thread {
            try {
                val url = URL("$BASE_URL/seferler")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader()
                    .readText()

                conn.disconnect()

                if (code in 200..299) {
                    val list = parseSeferler(body)

                    runOnUiThread {
                        allSeferler = list

                        // ✅ Eğer filter daha önce açıksa, dönünce de aynı filtreyi uygula
                        if (filterActive) {
                            applyFilter()
                        } else {
                            adapter.updateList(list.toMutableList())
                        }

                        Log.d("API_SEFER", "Seferler: $list")
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Seferler alınamadı: HTTP $code", Toast.LENGTH_LONG).show()
                        Log.e("API_SEFER", "HTTP $code body=$body")
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Sefer çekme FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("API_SEFER", "Sefer çekme hatası", e)
                }
            }
        }
    }

    private fun parseSeferler(json: String): List<Sefer> {
        val arr = JSONArray(json)
        val list = mutableListOf<Sefer>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                Sefer(
                    o.getInt("id"),
                    o.getString("from_"),
                    o.getString("to"),
                    o.getString("date"),
                    o.getString("time")
                )
            )
        }
        return list
    }
}
