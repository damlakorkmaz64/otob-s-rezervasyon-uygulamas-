package com.example.mobileproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SeferAdapter(
    private val seferList: MutableList<Sefer>,
    private val onClick: (Sefer) -> Unit
) : RecyclerView.Adapter<SeferAdapter.SeferViewHolder>() {

    class SeferViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoute: TextView = view.findViewById(R.id.tvRoute)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeferViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sefer, parent, false)
        return SeferViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeferViewHolder, position: Int) {
        val sefer = seferList[position]
        holder.tvRoute.text = "${sefer.from} → ${sefer.to}"
        holder.tvDateTime.text = "${sefer.date} - ${sefer.time}"

        holder.itemView.setOnClickListener {
            onClick(sefer)
        }
    }

    override fun getItemCount(): Int = seferList.size

    // ✅ Yeni liste basmak için
    fun updateList(newList: MutableList<Sefer>) {
        seferList.clear()
        seferList.addAll(newList)
        notifyDataSetChanged()
    }
}

