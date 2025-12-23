package com.example.popov402uc_22

import android.net.Uri

data class Note(
    val id: Int,
    val title: String,
    val description: String,
    val imageUri: Uri?,
    var isCompleted: Boolean = false
)
