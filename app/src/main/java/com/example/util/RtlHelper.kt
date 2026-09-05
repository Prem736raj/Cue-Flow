package com.example.util

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection

object RtlHelper {
    /**
     * Detects if the given text contains any strong RTL characters (e.g., Arabic, Hebrew, Urdu, Persian scripts).
     */
    fun isRtlText(text: String?): Boolean {
        if (text.isNullOrEmpty()) return false
        for (char in text) {
            val direction = Character.getDirectionality(char)
            if (direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                direction == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true
            }
        }
        return false
    }

    /**
     * Resolves whether the current text direction should be RTL or LTR given a setting
     * ('auto', 'ltr', 'rtl') and the actual text.
     */
    fun isRtl(directionSetting: String?, text: String?): Boolean {
        return when (directionSetting) {
            "rtl" -> true
            "ltr" -> false
            else -> isRtlText(text) // "auto" or null/default
        }
    }

    /**
     * Resolves the Compose [TextAlign] based on manual alignment, direction setting, and text content.
     */
    fun getTextAlign(alignmentSetting: String?, directionSetting: String?, text: String?): TextAlign {
        val isRtlResolved = isRtl(directionSetting, text)
        return when (alignmentSetting) {
            "center" -> TextAlign.Center
            "right" -> TextAlign.Right
            "left" -> TextAlign.Left
            else -> {
                // Default alignment: align Right if RTL is resolved, else Left
                if (isRtlResolved) TextAlign.Right else TextAlign.Left
            }
        }
    }

    /**
     * Resolves the Compose [TextDirection] to ensure proper character flow (e.g. mixed English + Arabic).
     */
    fun getTextDirection(directionSetting: String?, text: String?): TextDirection {
        return if (isRtl(directionSetting, text)) {
            TextDirection.Rtl
        } else {
            TextDirection.Ltr
        }
    }
}
