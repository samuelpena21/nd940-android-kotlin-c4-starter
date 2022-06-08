package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get: Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var reminderDataSource: ReminderDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setup() {
        reminderDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(reminderDataSource)
    }

    @Test
    fun loadReminders_loading() {
        // We pause the dispatcher to make the test run synchronously
        mainCoroutineRule.pauseDispatcher()
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showLoading.value).isEqualTo(true)
        // Then resume it to resume the coroutines
        mainCoroutineRule.resumeDispatcher()

        assertThat(remindersListViewModel.showLoading.value).isEqualTo(false)
    }

    @Test
    fun loadReminder_showNoData_shouldBeTrue() {
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showNoData.value).isEqualTo(true)
    }

    @Test
    fun loadReminder_showNoData_shouldBeFalse() = runBlockingTest {
        reminderDataSource.saveReminder(ReminderDTO("test", "test", "test", 0.0, 0.0))
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showNoData.value).isEqualTo(false)
    }
}