package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SaveReminderViewModelTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    //TODO: provide testing to the SaveReminderView and its live data objects
    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var saveReminderViewModel: SaveReminderViewModel

    @Before
    fun setup() {
        reminderDataSource = FakeDataSource()
        saveReminderViewModel = SaveReminderViewModel(reminderDataSource)
    }

    @Test
    fun setReminderData_validReminderItem() {
        assertThat(saveReminderViewModel.reminderData.value).isNull()

        val reminderDataItem = ReminderDataItem("test", "test", "test", 0.0, 0.0)
        saveReminderViewModel.setReminderData(reminderDataItem)

        assertThat(saveReminderViewModel.reminderData.getOrAwaitValue()).isNotNull()
    }

    @Test
    fun onClear_releaseLiveData() {
        saveReminderViewModel.setReminderData(
            ReminderDataItem(
                "test",
                "test",
                "test",
                9999.0,
                99999.0
            )
        )
        assertThat(saveReminderViewModel.reminderData.getOrAwaitValue()).isNotNull()

        saveReminderViewModel.onClear()

        assertThat(saveReminderViewModel.reminderData.getOrAwaitValue()).isNull()
    }

    @Test
    fun validateEnteredData_invalidData() {
        val reminderDataItem = ReminderDataItem("", "", "", 0.0, 0.0)

        val isValid = saveReminderViewModel.validateEnteredData(reminderDataItem)

        assertThat(isValid).isFalse()
    }

    @Test
    fun validateEnteredData_validTitle_invalidLocation() {
        val reminderDataItem = ReminderDataItem("Title", null, null, null, null)

        val isValid = saveReminderViewModel.validateEnteredData(reminderDataItem)

        assertThat(isValid).isFalse()
    }

    @Test
    fun validateEnteredData_isLocationSelected_false() {
        val reminderDataItem = ReminderDataItem("Title", null, "singapour", 21.87, 320.4)

        saveReminderViewModel.setReminderData(reminderDataItem)

        assertThat(saveReminderViewModel.isLocationSelected.getOrAwaitValue()).isTrue()
    }

    @Test
    fun validateEnteredData_validData() {
        val reminderDataItem = ReminderDataItem("title", "description", "tokio", 0.0, 0.0)

        val isValid = saveReminderViewModel.validateEnteredData(reminderDataItem)

        assertThat(isValid).isTrue()
    }

    @Test
    fun validateAndSaveReminder_validData() = runBlockingTest {
        val result = reminderDataSource.getReminders()
        val data = if (result is Result.Success) {
            result.data
        } else {
            emptyList()
        }
        assertThat(data.isEmpty()).isTrue()

        val reminderDataItem = ReminderDataItem("title", "description", "tokio", 0.0, 0.0)
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

        val resultAfterSave = reminderDataSource.getReminders()
        val dataAfterSave = if (resultAfterSave is Result.Success) {
            resultAfterSave.data
        } else {
            emptyList()
        }

        assertThat(dataAfterSave.isNotEmpty()).isTrue()
    }

    @Test
    fun validateAndSaveReminder_invalidData() = runBlockingTest {
        val result = reminderDataSource.getReminders()
        val data = if (result is Result.Success) {
            result.data
        } else {
            emptyList()
        }
        assertThat(data.isEmpty()).isTrue()

        val reminderDataItem = ReminderDataItem("", "", "", 0.0, 0.0)
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)

        val resultAfterSave = reminderDataSource.getReminders()
        val dataAfterSave = if (resultAfterSave is Result.Success) {
            resultAfterSave.data
        } else {
            emptyList()
        }

        assertThat(dataAfterSave.isEmpty()).isTrue()
    }

    @Test
    fun onFailure_validateToast() {
        val message = "There was an error"

        saveReminderViewModel.onFailure(message)

        assertThat(saveReminderViewModel.showToast.value).isEqualTo(message)
    }

    @Test
    fun delete_deletionSuccess() = runBlockingTest {
        val reminderDataItem = ReminderDataItem("title", "description", "Turkey", 10.0, 15.0)
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        val result = reminderDataSource.getReminders()
        val data = if (result is Result.Success) {
            result.data
        } else {
            emptyList()
        }
        assertThat(data.isNotEmpty()).isTrue()

        saveReminderViewModel.delete(reminderDataItem)

        val resultAfterDelete = reminderDataSource.getReminders()
        val dataAfterDelete = if (resultAfterDelete is Result.Success) {
            resultAfterDelete.data
        } else {
            emptyList()
        }
        assertThat(dataAfterDelete.isEmpty()).isTrue()
        assertThat(saveReminderViewModel.showToast.value).isEqualTo("Reminder deleted!")
    }

    @Test
    fun delete_deletionFailed() = runBlockingTest {
        val reminderDataItem = ReminderDataItem("title", "description", "Turkey", 10.0, 15.0)
        saveReminderViewModel.validateAndSaveReminder(reminderDataItem)
        val result = reminderDataSource.getReminders()
        val data = if (result is Result.Success) {
            result.data
        } else {
            emptyList()
        }
        assertThat(data.isNotEmpty()).isTrue()

        saveReminderViewModel.delete(ReminderDataItem("", "", "", null, null))

        val resultAfterDelete = reminderDataSource.getReminders()
        val dataAfterDelete = if (resultAfterDelete is Result.Success) {
            resultAfterDelete.data
        } else {
            emptyList()
        }

        assertThat(dataAfterDelete.isNotEmpty()).isTrue()
        assertThat(saveReminderViewModel.showToast.value).isEqualTo("Could not delete reminder")
    }

    @Test
    fun selectLocation_validLatLngAndLocation() {
        saveReminderViewModel.setReminderData(
            ReminderDataItem(
                "title",
                "description",
                null,
                null,
                null
            )
        )

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).isNull()

        val location = "Downtown Zone"
        val latLng = LatLng(123.4, 567.8)

        saveReminderViewModel.selectLocation(latLng, location)

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNotNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNotNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).isNotNull()
    }

    @Test
    fun selectLocation_validLatLngNullLocation() {
        saveReminderViewModel.setReminderData(
            ReminderDataItem(
                "title",
                "description",
                null,
                null,
                null
            )
        )

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).isNull()

        val location = null
        val latLng = LatLng(123.4, 567.8)

        saveReminderViewModel.selectLocation(latLng, location)

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNotNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNotNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).contains(latLng.latitude
            .toString())
    }

    @Test
    fun selectLocation_nullLatLngAndNullLocation() {
        saveReminderViewModel.setReminderData(
            ReminderDataItem(
                "title",
                "description",
                null,
                null,
                null
            )
        )

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).isNull()

        val location = null
        val latLng = null

        saveReminderViewModel.selectLocation(latLng, location)

        assertThat(saveReminderViewModel.reminderData.value?.latitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.longitude).isNull()
        assertThat(saveReminderViewModel.reminderData.value?.location).isEmpty()
    }
}