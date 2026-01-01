
package com.example.mobileproject

data class ReservationOut(
    val id: Int,
    val user_id: Int,
    val sefer_id: Int,
    val seat_number: Int,
    val created_at: String
)
