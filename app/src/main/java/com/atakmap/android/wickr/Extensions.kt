package com.atakmap.android.wickr

import com.wickr.android.api.WickrAPIObjects.WickrUser
import com.wickr.android.api.WickrAPIObjects.WickrMessage.TextMessage.MentionData
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

private val fileSizeFormat = DecimalFormat("#,##0.#")
private val units = arrayOf("B", "KB", "MB", "GB")

fun Long.formatAsSize(): String {
    if (this <= 0) return "0B"
    val digits = (log10(toDouble()) / log10(1024.toDouble())).toInt()
    val size = this / 1024.toDouble().pow(digits.toDouble())
    return "${fileSizeFormat.format(size)} ${units[digits]}"
}

fun WickrUser.initials(): String {
    val name = when {
        hasCustomName() -> customName
        hasFullName() -> fullName
        hasUsername() -> username
        else -> ""
    }

    return name.split(" ").filter {
        it.isNotEmpty()
    }.map { it.first() }.joinToString("")
}

fun WickrUser.primaryName(): String {
    return when {
        hasCustomName() -> customName
        hasFullName() -> fullName
        hasUsername() -> username
        else -> "Invalid Name"
    }
}


operator fun MentionData.component1(): WickrUser = user
operator fun MentionData.component2(): Int = startIndex
operator fun MentionData.component3(): Int = endIndex
