package org.gnome.adwaitakeyboard.editing

import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputConnection

/**
 * Tracks keyboard-initiated text edits so Undo and Redo work reliably inside
 * InputMethodService using batch InputConnection commands, while detecting if
 * an external app modifies the text field out-of-band.
 */
data class EditDelta(
    val cursorBefore: Int,
    val deletedText: String,
    val insertedText: String,
    val cursorAfter: Int
)

class EditHistoryManager {
    private val undoStack = ArrayDeque<EditDelta>()
    private val redoStack = ArrayDeque<EditDelta>()
    private var isPerformingUndoRedo = false

    fun clearHistory() {
        undoStack.clear()
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun recordInsertion(cursorBefore: Int, inserted: String) {
        if (isPerformingUndoRedo || inserted.isEmpty()) return
        redoStack.clear()
        // Merge single-character consecutive typing within the same word for natural Undo granularity
        val last = undoStack.lastOrNull()
        if (last != null &&
            last.deletedText.isEmpty() &&
            last.cursorAfter == cursorBefore &&
            inserted.length == 1 &&
            !inserted[0].isWhitespace() &&
            last.insertedText.length < 18
        ) {
            undoStack.removeLast()
            undoStack.addLast(
                last.copy(
                    insertedText = last.insertedText + inserted,
                    cursorAfter = cursorBefore + 1
                )
            )
        } else {
            pushUndo(
                EditDelta(
                    cursorBefore = cursorBefore,
                    deletedText = "",
                    insertedText = inserted,
                    cursorAfter = cursorBefore + inserted.length
                )
            )
        }
    }

    fun recordDeletion(cursorAfter: Int, deleted: String) {
        if (isPerformingUndoRedo || deleted.isEmpty()) return
        redoStack.clear()
        pushUndo(
            EditDelta(
                cursorBefore = cursorAfter + deleted.length,
                deletedText = deleted,
                insertedText = "",
                cursorAfter = cursorAfter
            )
        )
    }

    fun recordReplacement(cursorBeforeWord: Int, oldText: String, newText: String) {
        if (isPerformingUndoRedo) return
        redoStack.clear()
        pushUndo(
            EditDelta(
                cursorBefore = cursorBeforeWord + oldText.length,
                deletedText = oldText,
                insertedText = newText,
                cursorAfter = cursorBeforeWord + newText.length
            )
        )
    }

    private fun pushUndo(delta: EditDelta) {
        if (undoStack.size >= 50) {
            undoStack.removeFirst()
        }
        undoStack.addLast(delta)
    }

    /**
     * Attempts to undo the last recorded keyboard edit via InputConnection.
     * Falls back to android.R.id.undo if our local stack is empty and the host app supports it.
     */
    fun performUndo(ic: InputConnection?): Boolean {
        if (ic == null) return false
        val delta = undoStack.removeLastOrNull()
        if (delta == null) {
            return try {
                ic.performContextMenuAction(android.R.id.undo)
            } catch (_: Exception) {
                false
            }
        }

        isPerformingUndoRedo = true
        return try {
            ic.beginBatchEdit()
            val startPos = delta.cursorAfter - delta.insertedText.length
            if (startPos >= 0) {
                ic.setSelection(startPos, delta.cursorAfter)
                ic.commitText(delta.deletedText, 1)
            } else {
                if (delta.insertedText.isNotEmpty()) {
                    ic.deleteSurroundingText(delta.insertedText.length, 0)
                }
                if (delta.deletedText.isNotEmpty()) {
                    ic.commitText(delta.deletedText, 1)
                }
            }
            ic.endBatchEdit()
            redoStack.addLast(delta)
            true
        } catch (_: Exception) {
            false
        } finally {
            isPerformingUndoRedo = false
        }
    }

    fun performRedo(ic: InputConnection?): Boolean {
        if (ic == null) return false
        val delta = redoStack.removeLastOrNull()
        if (delta == null) {
            return try {
                ic.performContextMenuAction(android.R.id.redo)
            } catch (_: Exception) {
                false
            }
        }

        isPerformingUndoRedo = true
        return try {
            ic.beginBatchEdit()
            val startPos = delta.cursorBefore - delta.deletedText.length
            if (startPos >= 0) {
                ic.setSelection(startPos, delta.cursorBefore)
                ic.commitText(delta.insertedText, 1)
            } else {
                if (delta.deletedText.isNotEmpty()) {
                    ic.deleteSurroundingText(delta.deletedText.length, 0)
                }
                if (delta.insertedText.isNotEmpty()) {
                    ic.commitText(delta.insertedText, 1)
                }
            }
            ic.endBatchEdit()
            undoStack.addLast(delta)
            true
        } catch (_: Exception) {
            false
        } finally {
            isPerformingUndoRedo = false
        }
    }

    fun moveCursor(ic: InputConnection?, offset: Int): Boolean {
        if (ic == null) return false
        return try {
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted != null) {
                val textLen = extracted.text?.length ?: 0
                val currentStart = extracted.selectionStart
                val currentEnd = extracted.selectionEnd
                val base = if (currentStart != currentEnd) {
                    if (offset < 0) minOf(currentStart, currentEnd) else maxOf(currentStart, currentEnd)
                } else {
                    currentStart
                }
                val target = (base + offset).coerceIn(0, textLen)
                ic.setSelection(target, target)
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
