package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.Folder
import com.example.data.Script
import com.example.data.ScriptDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// Robolectric's API 36 preinstrumented jar requires JDK 21. Keep JVM tests
// compatible with the project's JDK 17 CI baseline while production compiles
// and targets API 36.
@Config(sdk = [35])
class ScriptDatabaseTest {

    private lateinit var database: ScriptDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ScriptDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `deleting a folder keeps its scripts and clears their folder assignment`() = runTest {
        val dao = database.scriptDao
        dao.insertFolder(Folder(name = "YouTube"))
        val scriptId = dao.insertScript(
            Script(
                title = "Launch video",
                content = "Opening line",
                folderName = "YouTube",
            ),
        ).toInt()

        dao.deleteFolderPreservingScripts(Folder(name = "YouTube"))

        val retainedScript = dao.getScriptById(scriptId)
        assertNotNull(retainedScript)
        assertNull(retainedScript?.folderName)
        assertEquals(emptyList<Folder>(), dao.getAllFolders().first())
    }
}
