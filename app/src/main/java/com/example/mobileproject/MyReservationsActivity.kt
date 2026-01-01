package com.example.mobileproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MyReservationsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reservations)

        val rv = findViewById<RecyclerView>(R.id.rvReservations)
        rv.layoutManager = LinearLayoutManager(this)

        val prefs = getSharedPreferences("reservations_prefs", MODE_PRIVATE)
        val json = prefs.getString("reservations", null)

        val type = object : TypeToken<List<Reservation>>() {}.type
        val reservations: List<Reservation> =
            if (json != null) Gson().fromJson(json, type) else emptyList()

        rv.adapter = ReservationAdapter(reservations)
    }
}
