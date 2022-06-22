package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

class FakeAndroidReminderRepository : ReminderDataSource {

    var reminderServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        return Result.Success(reminderServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test Exception")
        }
        return when (val reminder = reminderServiceData.values.find { it.id == id }) {
            null -> Result.Error("Could not find reminder")
            else -> Result.Success(reminder)
        }
    }

    override suspend fun deleteReminder(reminder: ReminderDTO): Result<Boolean> {
        return if (shouldReturnError) {
            Result.Error("Reminder does not exists")
        } else {
            reminderServiceData.remove(reminder.id)
            Result.Success(true)
        }
    }

    override suspend fun deleteAllReminders() {
        reminderServiceData.clear()
    }
}