package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getAndSaveReminder() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        database.reminderDao().saveReminder(reminder)

        val result = database.reminderDao().getReminderById(reminder.id)

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(reminder.id)
        assertThat(result?.title).isEqualTo(reminder.title)
        assertThat(result?.description).isEqualTo(reminder.description)
    }

    @Test
    fun getAllReminders() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        val reminder2 = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        val reminder3 = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        val result = database.reminderDao().getReminders()

        assertThat(result.size).isEqualTo(3)
    }

    @Test
    fun saveTheSameInstanceTwice() = runBlockingTest {
        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder)

        val result = database.reminderDao().getReminders()

        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun deleteReminder() = runBlockingTest {
        val result = database.reminderDao().getReminders()
        assertThat(result.isEmpty()).isTrue()

        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        database.reminderDao().saveReminder(reminder)
        val resultAfterSave = database.reminderDao().getReminders()
        assertThat(resultAfterSave.size).isEqualTo(1)

        database.reminderDao().deleteReminder(reminder)

        val resultAfterDelete = database.reminderDao().getReminders()
        assertThat(resultAfterDelete.isEmpty()).isTrue()
    }

    @Test
    fun deleteAllReminder() = runBlockingTest {
        val result = database.reminderDao().getReminders()
        assertThat(result.isEmpty()).isTrue()

        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        val reminder2 = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder2)

        val resultAfterSave = database.reminderDao().getReminders()
        assertThat(resultAfterSave.size).isEqualTo(2)

        database.reminderDao().deleteAllReminders()

        val resultAfterDelete = database.reminderDao().getReminders()
        assertThat(resultAfterDelete.isEmpty()).isTrue()
    }
}