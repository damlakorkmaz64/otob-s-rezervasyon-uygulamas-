package com.example.mobileproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReservationAdapter(
    private val list: List<Reservation>
) : RecyclerView.Adapter<ReservationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoute: TextView = view.findViewById(R.id.tvRoute)
        val tvDate: TextView = view.findViewById(R.id.tvDateTime)
        val tvSeat: TextView = view.findViewById(R.id.tvSeat)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = list[position]
        holder.tvRoute.text = "${r.from} → ${r.to}"
        holder.tvDate.text = "${r.date} ${r.time}"
        holder.tvSeat.text = "Koltuk: ${r.seatNumber}"
    }

    override fun getItemCount() = list.size
}

