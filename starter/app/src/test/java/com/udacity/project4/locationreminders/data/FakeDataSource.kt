package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val list: MutableList<ReminderDTO> = mutableListOf()) :
    ReminderDataSource {

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return Result.Success(list.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        list.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return when (val reminder = list.find { it.id == id }) {
            null -> Result.Error("Could not find reminder")
            else -> Result.Success(reminder)
        }
    }

    override suspend fun deleteReminder(reminder: ReminderDTO): Result<Boolean> {
        return if (list.remove(reminder)) {
            Result.Success(true)
        } else {
            Result.Error("Reminder does not exists")
        }
    }

    override suspend fun deleteAllReminders() {
        list.clear()
    }
}