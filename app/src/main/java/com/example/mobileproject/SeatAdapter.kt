package com.example.mobileproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class SeatAdapter(
    private val seats: List<Seat>,
    private val onSeatClick: (Seat) -> Unit
) : RecyclerView.Adapter<SeatAdapter.SeatViewHolder>() {

    inner class SeatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSeat: TextView = view.findViewById(R.id.tvSeat)
        val card: CardView = view as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_seat, parent, false)
        return SeatViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeatViewHolder, position: Int) {
        val seat = seats[position]
        holder.tvSeat.text = seat.number.toString()

        val context = holder.itemView.context

        // 🎨 Renkler
        when {
            seat.isReserved -> {
                holder.card.setCardBackgroundColor(context.getColor(R.color.seat_reserved_bg))
                holder.tvSeat.setTextColor(context.getColor(R.color.seat_reserved_text))
            }

            seat.isSelected -> {
                holder.card.setCardBackgroundColor(context.getColor(R.color.seat_selected_bg))
                holder.tvSeat.setTextColor(context.getColor(R.color.seat_selected_text))
            }

            else -> {
                holder.card.setCardBackgroundColor(context.getColor(R.color.seat_available_bg))
                holder.tvSeat.setTextColor(context.getColor(R.color.seat_available_text))
            }
        }

        // ✅ Reserved koltuk: tıklanamaz + daha “disabled” dursun
        holder.itemView.isEnabled = !seat.isReserved
        holder.itemView.alpha = if (seat.isReserved) 0.45f else 1.0f

        // ✅ Click davranışı
        holder.itemView.setOnClickListener(null)
        if (!seat.isReserved) {
            holder.itemView.setOnClickListener {
                onSeatClick(seat)
                // seçim durumları değiştiği için listeyi yenile
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int = seats.size
}

