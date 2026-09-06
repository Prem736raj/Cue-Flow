package com.example.data

import kotlinx.coroutines.flow.Flow

class ScriptRepository(private val scriptDao: ScriptDao) {
    val allScripts: Flow<List<Script>> = scriptDao.getAllScripts()
    val allFolders: Flow<List<Folder>> = scriptDao.getAllFolders()

    suspend fun insertFolder(folder: Folder) {
        scriptDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folder: Folder) {
        scriptDao.deleteFolderPreservingScripts(folder)
    }

    suspend fun getScriptById(id: Int): Script? = scriptDao.getScriptById(id)

    suspend fun insert(script: Script): Long = scriptDao.insertScript(script)

    suspend fun update(script: Script) {
        scriptDao.updateScript(script)
    }

    suspend fun delete(script: Script) {
        scriptDao.deleteScript(script)
    }

    suspend fun deleteById(id: Int) {
        scriptDao.deleteScriptById(id)
    }
}
