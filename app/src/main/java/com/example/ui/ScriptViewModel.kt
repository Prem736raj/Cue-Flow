package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Script
import com.example.data.Folder
import com.example.data.ScriptDatabase
import com.example.data.ScriptRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScriptViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ScriptRepository

    val allScripts: StateFlow<List<Script>>
    val allFolders: StateFlow<List<Folder>>

    init {
        val database = ScriptDatabase.getDatabase(application)
        repository = ScriptRepository(database.scriptDao)
        allScripts = repository.allScripts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        allFolders = repository.allFolders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertFolder(com.example.data.Folder(name = name.trim()))
            }
        }
    }

    fun deleteFolder(folder: com.example.data.Folder) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun moveScriptToFolder(script: Script, folderName: String?) {
        viewModelScope.launch {
            val updated = script.copy(folderName = folderName, updatedAt = System.currentTimeMillis())
            repository.update(updated)
        }
    }

    fun addScript(
        title: String,
        content: String,
        speed: Int = 5,
        fontSize: Int = 24,
        isMirrored: Boolean = false
    ) {
        viewModelScope.launch {
            if (title.isNotBlank()) {
                repository.insert(
                    Script(
                        title = title.trim(),
                        content = content.trim(),
                        scrollSpeed = speed,
                        fontSize = fontSize,
                        isMirrored = isMirrored,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateScript(script: Script) {
        viewModelScope.launch {
            repository.update(script.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteScript(script: Script) {
        viewModelScope.launch {
            repository.delete(script)
        }
    }

    fun deleteScriptById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    suspend fun getScriptById(id: Int): Script? {
        return repository.getScriptById(id)
    }

    suspend fun saveScript(script: Script): Long {
        val sanitized = script.copy(
            title = if (script.title.isBlank()) "Untitled Script" else script.title.trim(),
            content = script.content.trim(),
            updatedAt = System.currentTimeMillis()
        )
        return if (sanitized.id == 0) {
            repository.insert(sanitized)
        } else {
            repository.update(sanitized)
            sanitized.id.toLong()
        }
    }
}
