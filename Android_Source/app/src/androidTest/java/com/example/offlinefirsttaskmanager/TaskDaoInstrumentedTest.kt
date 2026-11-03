package com.example.offlinefirsttaskmanager

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.offlinefirsttaskmanager.data.local.AppDatabase
import com.example.offlinefirsttaskmanager.data.local.TaskDao
import com.example.offlinefirsttaskmanager.data.local.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskDaoInstrumentedTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun prepare() {
        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.taskDao()
    }

    @After
    fun cleanup() {
        database.close()
    }

    @Test
    fun insertTaskIsVisibleThroughFlow() = runBlocking {
        val task = TaskEntity(id = "room-01", title = "Offline new")
        dao.insertTask(task)
        assertEquals(listOf("room-01"), dao.getAllTasks().first().map { it.id })
        assertEquals("PENDING", dao.getTaskById("room-01")?.syncStatus)
    }

    @Test
    fun updateTaskKeepsOfflineData() = runBlocking {
        val original = TaskEntity(id = "room-02", title = "Before", updatedAt = 100)
        dao.insertTask(original)
        dao.updateTask(original.copy(title = "After", updatedAt = 101))
        val result = dao.getTaskById("room-02")
        assertEquals("After", result?.title)
        assertEquals(101L, result?.updatedAt)
    }

    @Test
    fun deleteIsTombstoneAndHiddenFromVisibleList() = runBlocking {
        val original = TaskEntity(id = "room-03", title = "Delete offline")
        dao.insertTask(original)
        dao.deleteTask("room-03", original.updatedAt + 1)
        val stored = dao.getTaskById("room-03")
        assertNotNull(stored)
        assertTrue(stored!!.isDeleted)
        assertEquals("PENDING", stored.syncStatus)
        assertTrue(dao.getAllTasks().first().isEmpty())
    }

    @Test
    fun syncingAndConfirmingUpdatesVersion() = runBlocking {
        val task = TaskEntity(id = "room-04", title = "Upload", updatedAt = 100, serverVersion = 0)
        dao.insertTask(task)
        assertEquals(1, dao.markSyncing("room-04", 100))
        assertEquals(1, dao.confirmSynced("room-04", 100, 1))
        val result = dao.getTaskById("room-04")
        assertEquals("SYNCED", result?.syncStatus)
        assertEquals(1L, result?.serverVersion)
    }

    @Test
    fun changingTaskDuringUploadLeavesPendingStatus() = runBlocking {
        val task = TaskEntity(id = "room-05", title = "Original", updatedAt = 100)
        dao.insertTask(task)
        assertEquals(1, dao.markSyncing("room-05", 100))
        dao.updateTask(task.copy(title = "New offline value", updatedAt = 101, syncStatus = "PENDING"))
        dao.confirmSynced("room-05", 100, 1)
        val result = dao.getTaskById("room-05")
        assertEquals("PENDING", result?.syncStatus)
        assertEquals("New offline value", result?.title)
        assertEquals(1L, result?.serverVersion)
    }

    @Test
    fun recreatedServerVersionCanGoBackToOne() = runBlocking {
        val task = TaskEntity(
            id = "room-07",
            title = "Pending after server reset",
            updatedAt = 100L,
            serverVersion = 4L,
            syncStatus = "PENDING"
        )
        dao.insertTask(task)
        dao.markSyncing("room-07", 100L)
        val affected = dao.confirmRecreated("room-07", 100L, 4L, 1L)
        assertEquals(1, affected)
        assertEquals(1L, dao.getTaskById("room-07")?.serverVersion)
        assertEquals("SYNCED", dao.getTaskById("room-07")?.syncStatus)
    }

    @Test
    fun persistentRoomDatabaseSurvivesCloseAndReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filename = "offlinefirst-persist-${System.nanoTime()}"
        val first = Room.databaseBuilder(context, AppDatabase::class.java, filename).build()
        try {
            first.taskDao().insertTask(TaskEntity(id = "persist-01", title = "Survives restart"))
        } finally {
            first.close()
        }

        val reopened = Room.databaseBuilder(context, AppDatabase::class.java, filename).build()
        try {
            assertEquals("Survives restart", reopened.taskDao().getTaskById("persist-01")?.title)
        } finally {
            reopened.close()
            context.deleteDatabase(filename)
        }
    }

    @Test
    fun localTombstoneNeverReturnsToVisibleList() = runBlocking {
        val task = TaskEntity(id = "room-06", title = "Deleted", updatedAt = 100)
        dao.insertTask(task)
        dao.deleteTask("room-06", 101)
        assertEquals(1, dao.markLocalDeleteSynced("room-06", 101))
        assertEquals("SYNCED", dao.getTaskById("room-06")?.syncStatus)
        assertFalse(dao.getAllTasks().first().any { it.id == "room-06" })
    }
}
