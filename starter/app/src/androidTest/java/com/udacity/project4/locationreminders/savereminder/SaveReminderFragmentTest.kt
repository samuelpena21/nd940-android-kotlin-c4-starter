package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidReminderRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class SaveReminderFragmentTest : AutoCloseKoinTest() {
    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()
        appContext = ApplicationProvider.getApplicationContext()
        val myModule = module(override = true) {
            viewModel {
                SaveReminderViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    get() as ReminderDataSource
                )
            }
            single { FakeAndroidReminderRepository() as ReminderDataSource }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun createReminder_emptyTitle_validateSnackBar() = runBlockingTest {
        // Given - You are in the SaveReminder fragment
        launchFragmentInContainer<SaveReminderFragment>(null, R.style.AppTheme)

        // When - Clicking the save button with the title, description and location not set
        Espresso.onView(withId(R.id.saveReminder))
            .perform(click())

        // Then - A snackBar with a message should appear on screen
        Espresso.onView(withText(R.string.err_enter_title))
            .check(ViewAssertions.matches(isDisplayed()))

        Thread.sleep(2000)
    }

    @Test
    fun createReminder_emptyLocation_validateSnackBar() = runBlockingTest {
        // Given - You are in the SaveReminder fragment
        launchFragmentInContainer<SaveReminderFragment>(null, R.style.AppTheme)
        Espresso.onView(withId(R.id.reminderTitle))
            .perform(typeText("Title 1"), closeSoftKeyboard())

        // When - Clicking the save button with an empty location
        Espresso.onView(withId(R.id.saveReminder))
            .perform(click())

        // Then - A snackBar with a message should appear on screen
        Espresso.onView(withText(R.string.err_select_location))
            .check(ViewAssertions.matches(isDisplayed()))

        Thread.sleep(2000)
    }

    /*
    * Since we cannot interact with the google map using Espresso, and the way I changed the code
    * do not let you pass the selectLocationFragment without a valid location, I decided to just
    * to jump this test for now. Later I would make it using the recommended way to test Google
    * Maps that is using UI Automator:  https://developer.android
    * .com/training/testing/other-components/ui-automator
    *
    @Test
    fun createReminder_validReminder_navigateBack() = runBlockingTest {
    }

     */
}