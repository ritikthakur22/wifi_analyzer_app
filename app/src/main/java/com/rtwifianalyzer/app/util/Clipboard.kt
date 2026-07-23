package com.rtwifianalyzer.app.util

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString

fun ClipboardManager.copy(value: String) = setText(AnnotatedString(value))
