package com.vocabulary.app.ui.admin

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.vocabulary.app.data.remote.AdminException
import com.vocabulary.app.data.remote.ImportPreview
import com.vocabulary.app.data.remote.WordAdmin
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AdminUiState(
    val email: String = "",
    val busy: Boolean = false,
    val busyText: String = "",
    val fileName: String = "",
    /** Set after a file was read: the person confirms before anything is written. */
    val preview: ImportPreview? = null,
    /** One-shot message for the snackbar. */
    val message: String? = null
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val admin: WordAdmin
) : ViewModel() {

    private val _state = MutableStateFlow(
        AdminUiState(email = runCatching { FirebaseAuth.getInstance().currentUser?.email }.getOrNull().orEmpty())
    )
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    fun messageShown() = _state.update { it.copy(message = null) }
    fun dismissPreview() = _state.update { it.copy(preview = null) }

    /** Step 1 of an import: read the picked file and show what would change. */
    fun onFilePicked(uri: Uri, name: String) {
        if (_state.value.busy) return
        run("Reading the file...") {
            val text = withContext(Dispatchers.IO) { readText(uri) }
            val preview = admin.preview(text)
            _state.update { it.copy(preview = preview, fileName = name) }
        }
    }

    /** Step 2: the person confirmed. */
    fun confirmImport() {
        val preview = _state.value.preview ?: return
        if (_state.value.busy) return
        _state.update { it.copy(preview = null) }
        run("Saving to the database...") {
            val total = admin.apply(preview)
            _state.update { it.copy(message = "Done. $total words in the database.") }
        }
    }

    fun export(uri: Uri) {
        if (_state.value.busy) return
        run("Preparing the file...") {
            val text = admin.exportJson()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
                    ?: throw AdminException("Could not write the file.")
            }
            _state.update { it.copy(message = "words.json saved.") }
        }
    }

    private fun readText(uri: Uri): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw AdminException("Could not open the file.")
        if (bytes.size > MAX_FILE_BYTES) throw AdminException("The file is too large (limit 25 MB).")
        return String(bytes, Charsets.UTF_8).removePrefix("﻿")
    }

    private fun run(text: String, block: suspend () -> Unit) {
        _state.update { it.copy(busy = true, busyText = text) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: AdminException) {
                _state.update { it.copy(message = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Something went wrong: ${e.message ?: e.javaClass.simpleName}") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private companion object {
        const val MAX_FILE_BYTES = 25 * 1024 * 1024
    }
}
