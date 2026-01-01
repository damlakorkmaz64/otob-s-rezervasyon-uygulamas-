package com.example.mobileproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminSeferAdapter(
    private var seferler: MutableList<Sefer>,
    private val onDeleteClick: (Sefer) -> Unit
) : RecyclerView.Adapter<AdminSeferAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvInfo: TextView = view.findViewById(R.id.tvSeferInfo)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete) // ✅ değişti
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_sefer, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = seferler.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sefer = seferler[position]
        holder.tvInfo.text = "${sefer.from} → ${sefer.to} | ${sefer.date} ${sefer.time}"

        holder.btnDelete.setOnClickListener {
            onDeleteClick(sefer)
        }
    }

    fun updateList(newList: MutableList<Sefer>) {
        seferler = newList
        notifyDataSetChanged()
    }
}

