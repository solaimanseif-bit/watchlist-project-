package com.example.ui.util

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus

/**
 * A safe TextToolbar implementation that avoids Android DecorView FloatingActionMode
 * lifecycle conflicts and "Destroying unexpected ActionMode instance of TYPE_FLOATING" errors.
 */
object SafeTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        // Intentionally no-op to prevent FloatingActionMode creation in sub-windows and dialogs
    }

    override fun hide() {
        // No-op
    }
}
