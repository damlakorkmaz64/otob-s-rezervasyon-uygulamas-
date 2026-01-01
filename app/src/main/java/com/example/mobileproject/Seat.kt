package com.example.mobileproject


data class Seat(
    val number: Int,
    var isReserved: Boolean = false,
    var isSelected: Boolean = false
)
