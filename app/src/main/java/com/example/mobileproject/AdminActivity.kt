package com.example.mobileproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class AdminActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdminSeferAdapter

    private val BASE_URL = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        recyclerView = findViewById(R.id.rvAdminSeferler)
        val btnAdd = findViewById<Button>(R.id.btnAddSefer)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AdminSeferAdapter(mutableListOf()) { sefer ->
            // backend delete
            deleteSeferFromBackend(sefer.id)
        }
        recyclerView.adapter = adapter

        // ➕ SEFER EKLE
        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddSeferActivity::class.java))
        }

        // 🔴 LOGOUT
        btnLogout.setOnClickListener {
            val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
            prefs.edit().clear().apply()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // ilk yükleme
        fetchSeferler()
    }

    override fun onResume() {
        super.onResume()
        // AddSeferActivity'den dönünce güncelle
        fetchSeferler()
    }

    private fun fetchSeferler() {
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
                    .bufferedReader().readText()

                conn.disconnect()

                if (code !in 200..299) throw RuntimeException("HTTP $code: $body")

                val list = parseSeferler(body)

                runOnUiThread {
                    adapter.updateList(list.toMutableList())
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Seferler alınamadı: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ADMIN_SEFER_LIST", "Exception", e)
                }
            }
        }
    }

    private fun deleteSeferFromBackend(seferId: Int) {
        thread {
            try {
                val url = URL("$BASE_URL/admin/seferler/$seferId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().readText()

                conn.disconnect()

                runOnUiThread {
                    if (code in 200..299) {
                        Toast.makeText(this, "Sefer silindi ✅", Toast.LENGTH_SHORT).show()
                        fetchSeferler() // listeyi yenile
                    } else {
                        Toast.makeText(this, "Silme başarısız: HTTP $code", Toast.LENGTH_LONG).show()
                        Log.e("ADMIN_SEFER_DEL", "HTTP $code body=$body")
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Silme FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("ADMIN_SEFER_DEL", "Exception", e)
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
                    id = o.getInt("id"),
                    from = o.getString("from_"),
                    to = o.getString("to"),
                    date = o.getString("date"),
                    time = o.getString("time")
                )
            )
        }
        return list
    }
}
