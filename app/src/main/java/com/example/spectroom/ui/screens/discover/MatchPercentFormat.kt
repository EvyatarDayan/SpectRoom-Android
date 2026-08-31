package com.example.spectroom.ui.screens.discover

/** Formats match percentage digits with spaces, e.g. 72 -> "7 2 %" (iOS parity). */
fun formatSpacedMatchPercent(percentage: Int): String {
    return percentage.coerceIn(0, 100)
        .toString()
        .toCharArray()
        .joinToString(" ") + " %"
}
