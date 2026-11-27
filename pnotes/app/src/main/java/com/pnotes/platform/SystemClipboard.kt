package com.pnotes.platform

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri

/**
 * Image clipboard access. The clipboard is read through this rather than directly, so:
 *
 *  - SVG content sitting on the clipboard as copied markup (e.g. from an editor) can be inserted
 *    as bytes just like a copied bitmap, with no separate "paste SVG" path;
 *  - the uri/type checks live in one place;
 *  - tests can stub it.
 */
object SystemClipboard {

    /** An image the clipboard is holding by uri, or null when it holds something else. */
    fun imageUri(context: Context): Uri? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        val uri = clip.getItemAt(0).uri ?: return null
        val type = context.contentResolver.getType(uri)
        val isImage = type?.startsWith("image/") == true || clip.description?.hasMimeType("image/*") == true
        return if (isImage) uri else null
    }

    /** SVG markup sitting on the clipboard as plain text (copied source), as insertable bytes. */
    fun svgBytes(context: Context): ByteArray? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
        val text = cm.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString() ?: return null
        val t = text.trimStart('\uFEFF').trimStart()
        val looksSvg = t.startsWith("<svg") || ((t.startsWith("<?xml") || t.startsWith("<!")) && t.contains("<svg"))
        return if (looksSvg) text.toByteArray() else null
    }

    /** Whether there is anything here worth offering a paste for. */
    fun hasImage(context: Context): Boolean = imageUri(context) != null || svgBytes(context) != null

    /** The clipboard's image as bytes, reading the uri or falling back to copied SVG source. */
    fun imageBytes(context: Context): ByteArray? {
        imageUri(context)?.let { uri ->
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                .getOrNull()
                ?.let { return it }
        }
        return svgBytes(context)
    }
}
