package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidReminderRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
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
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    @Before
    fun init() {
        stopKoin()
        appContext = getApplicationContext()
        val myModule = module(override = true) {
            viewModel {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single { FakeAndroidReminderRepository() }
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
    fun noData_displaysNoDataLabel() = runBlockingTest {
        // When - Opening the ReminderListFragment
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        // Then - Displays a no data message if no data is added
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        Thread.sleep(2000)
    }

    @Test
    fun data_available() = runBlockingTest {
        repository.saveReminder(
            ReminderDTO(
                title = "title",
                description = "description",
                location = "Brazil",
                latitude = 3.4,
                longitude = 4.3
            )
        )

        // Given - On the ReminderListFragment
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)

        // Then - Validate that the data is displayed
        onView(withId(R.id.reminderssRecyclerView))
            .perform(
                RecyclerViewActions
                    .scrollTo<RecyclerView.ViewHolder>(
                        hasDescendant(withText("title"))
                    )
            )
    }

    @Test
    fun navigateToSaveReminder() = runBlockingTest {
        // Given - On the ReminderListFragment
        val navController = Mockito.mock(NavController::class.java)
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        scenario.onFragment {
            it.view?.let { view ->
                Navigation.setViewNavController(view, navController)
            }
        }

        // When - Clicking on the FAB
        onView(withId(R.id.addReminderFAB))
            .perform(click())

        // Then - Verify that we navigate to the other screen
        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder(null)
        )
    }
}