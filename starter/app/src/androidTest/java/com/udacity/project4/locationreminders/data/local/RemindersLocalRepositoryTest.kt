package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var localDataSource: ReminderDataSource
    private lateinit var database: RemindersDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource = RemindersLocalRepository(
            remindersDao = database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun closeDb() {
        database.close()

    }

    @Test
    fun getAndSaveReminder() = runBlocking {
        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        localDataSource.saveReminder(reminder)
        database.reminderDao().saveReminder(reminder)

        val result = localDataSource.getReminder(reminder.id)

        result as Result.Success
        Truth.assertThat(result.data).isNotNull()
        Truth.assertThat(result.data.id).isEqualTo(reminder.id)
        Truth.assertThat(result.data.title).isEqualTo(reminder.title)
        Truth.assertThat(result.data.description).isEqualTo(reminder.description)
    }

    @Test
    fun getAllReminders() = runBlocking {
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

        localDataSource.saveReminder(reminder)
        localDataSource.saveReminder(reminder2)
        localDataSource.saveReminder(reminder3)

        val result = localDataSource.getReminders()

        result as Result.Success
        Truth.assertThat(result.data.size).isEqualTo(3)
    }

    @Test
    fun saveTheSameInstanceTwice() = runBlocking {
        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        localDataSource.saveReminder(reminder)
        localDataSource.saveReminder(reminder)

        val result = localDataSource.getReminders()

        result as Result.Success
        Truth.assertThat(result.data.size).isEqualTo(1)
    }

    @Test
    fun deleteReminder() = runBlocking {
        val result = localDataSource.getReminders()
        result as Result.Success
        Truth.assertThat(result.data.isEmpty()).isTrue()

        val reminder = ReminderDTO(
            title = "reminder",
            description = "description",
            location = "florida",
            latitude = 12.1,
            longitude = 13.3
        )

        localDataSource.saveReminder(reminder)
        val resultAfterSave = localDataSource.getReminders()

        resultAfterSave as Result.Success
        Truth.assertThat(resultAfterSave.data.size).isEqualTo(1)

        localDataSource.deleteReminder(reminder)

        val resultAfterDelete = localDataSource.getReminders()
        resultAfterDelete as Result.Success
        Truth.assertThat(resultAfterDelete.data.isEmpty()).isTrue()
    }

    @Test
    fun deleteAllReminder() = runBlocking {
        val result = localDataSource.getReminders()
        result as Result.Success
        Truth.assertThat(result.data.isEmpty()).isTrue()

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

        localDataSource.saveReminder(reminder)
        localDataSource.saveReminder(reminder2)

        val resultAfterSave = localDataSource.getReminders()
        resultAfterSave as Result.Success
        Truth.assertThat(resultAfterSave.data.size).isEqualTo(2)

        localDataSource.deleteAllReminders()

        val resultAfterDelete = localDataSource.getReminders()
        resultAfterDelete as Result.Success
        Truth.assertThat(resultAfterDelete.data.isEmpty()).isTrue()
    }
}