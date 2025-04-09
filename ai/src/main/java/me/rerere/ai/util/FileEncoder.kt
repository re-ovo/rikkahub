package me.rerere.ai.util

import android.net.Uri
import android.util.Base64
import me.rerere.ai.ui.UIMessagePart
import java.io.File

fun UIMessagePart.Image.encodeBase64(): Result<String> = runCatching {
    if(this.url.startsWith("file://")) {
        val file = File(Uri.parse(this.url).path)
        if (file.exists()) {
            val bytes = file.readBytes()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/*;base64,$encoded"
        } else {
            throw IllegalArgumentException("File does not exist: ${this.url}")
        }
    } else {
        throw IllegalArgumentException("Unsupported URL format: $url")
    }
}