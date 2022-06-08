package com.udacity.project4.locationreminders.savereminder

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.launch

class SaveReminderViewModel(private val dataSource: ReminderDataSource) :
    BaseViewModel() {

    private var _reminderData = MutableLiveData<ReminderDataItem>()
    val reminderData = _reminderData

    private val _latLng = MutableLiveData<LatLng>()

    val isLocationSelected = Transformations.map(_latLng) { data ->
        // The latitude and longitude should not be null nor 0.0
        data != null
    }

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        _reminderData.value = null
        _latLng.value = null
    }

    fun setReminderData(reminderData: ReminderDataItem) {
        _reminderData.value = reminderData
        val longitude = reminderData.longitude
        val latitude = reminderData.latitude

        if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
            _latLng.value = LatLng(latitude, longitude)
        }
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        }
    }

    /**
     * Save the reminder to the data source
     */
    private fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            showLoading.value = false
            showToast.value = "Reminder Saved !"
            navigationCommand.value = NavigationCommand.Back
        }
    }

    fun onFailure(errorMessage: String) {
        showToast.value = "There was an error"
        Log.d("SaveReminderViewModel", "There was an error: $errorMessage")
    }

    fun selectLocation(latLng: LatLng?, location: String?) {
        _reminderData.value?.latitude = latLng?.latitude
        _reminderData.value?.longitude = latLng?.longitude

        val latLngString = if (latLng != null) {
            "${latLng.latitude} ${latLng.longitude}"
        } else {
            ""
        }

        _reminderData.value?.location = location ?: latLngString
        latLng?.let {
            _latLng.value = it
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    fun delete(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            val result = dataSource.deleteReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            when (result) {
                is Result.Error -> {
                    showToast.value = "Could not delete reminder"
                }
                is Result.Success -> {
                    showToast.value = "Reminder deleted!"
                }
            }
            showLoading.value = false
            navigationCommand.value = NavigationCommand.Back
        }
    }
}