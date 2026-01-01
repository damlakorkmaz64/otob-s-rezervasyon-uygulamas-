package com.example.mobileproject

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ReservationHistoryAdapter(
    private val reservations: MutableList<ReservationOut>
) : RecyclerView.Adapter<ReservationHistoryAdapter.ViewHolder>() {

    // emülatör için
    private val BASE_URL = "http://10.0.2.2:8000"

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRoute: TextView = view.findViewById(R.id.tvRoute)
        val tvDateTime: TextView = view.findViewById(R.id.tvDateTime)
        val tvSeat: TextView = view.findViewById(R.id.tvSeat)
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reservation_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val r = reservations[position]

        // Artık sefer detayları yok; hızlı ve net gösterelim
        holder.tvRoute.text = "Sefer ID: ${r.sefer_id}"
        holder.tvDateTime.text = "Oluşturma: ${r.created_at}"
        holder.tvSeat.text = "Koltuk: ${r.seat_number}"

        holder.btnCancel.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            val ctx = holder.itemView.context

            // UI kilitlemek istersen:
            holder.btnCancel.isEnabled = false
            holder.btnCancel.text = "İptal ediliyor..."

            deleteReservation(
                reservationId = r.id,
                onSuccess = {
                    val newPos = holder.bindingAdapterPosition
                    if (newPos != RecyclerView.NO_POSITION && newPos < reservations.size) {
                        reservations.removeAt(newPos)
                        notifyItemRemoved(newPos)
                    } else {
                        // fallback: id ile bul
                        val idx = reservations.indexOfFirst { it.id == r.id }
                        if (idx != -1) {
                            reservations.removeAt(idx)
                            notifyItemRemoved(idx)
                        }
                    }
                    Toast.makeText(ctx, "Rezervasyon iptal edildi ✅", Toast.LENGTH_SHORT).show()
                },
                onError = { msg ->
                    holder.btnCancel.isEnabled = true
                    holder.btnCancel.text = "İptal Et"
                    Toast.makeText(ctx, "İptal başarısız: $msg", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    override fun getItemCount(): Int = reservations.size

    private fun deleteReservation(
        reservationId: Int,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        thread {
            try {
                val url = URL("$BASE_URL/reservations/$reservationId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader().readText()

                conn.disconnect()

                val runUi = {
                    if (code in 200..299) onSuccess()
                    else onError("HTTP $code: $body")
                }

                // Adapter'da runOnUiThread yok; holder context’inin activity’si üzerinden koşacağız:
                // En güvenlisi: main thread’e post
                android.os.Handler(android.os.Looper.getMainLooper()).post { runUi() }

            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Log.e("RES_DELETE", "Exception", e)
                    onError(e.message ?: "Unknown error")
                }
            }
        }
    }
}
