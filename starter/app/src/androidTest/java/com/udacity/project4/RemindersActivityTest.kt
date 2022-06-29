package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.espresso.Espresso.pressBack
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin
    // after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to
     * test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
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

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun createReminder_error_no_title() = runBlockingTest {
        // Set initial State
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Setup the activity to monitor
        dataBindingIdlingResource.monitorActivity(scenario)

        // Validate initial state
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))

        // Given - When we are located in the ReminderList Fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // When - We are in the Save Reminder Screen
        onView(withId(R.id.saveReminder)).perform(click())

        // Then - An error message should appear
        onView(withText(R.string.err_enter_title)).check(ViewAssertions.matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun createReminder_error_no_location() = runBlockingTest {
        // Set initial State
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Setup the activity to monitor
        dataBindingIdlingResource.monitorActivity(scenario)

        // Validate initial state
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))

        // Given - When we are located in the ReminderList Fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // When - We are in the Save Reminder Screen
        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title 1"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Description 1"))

        onView(withId(R.id.saveReminder)).perform(click())

        // Then - An error message should appear
        onView(withText(R.string.err_select_location)).check(ViewAssertions.matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun createReminder_happyPath() = runBlockingTest {
        // Set initial State
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        // Setup the activity to monitor
        dataBindingIdlingResource.monitorActivity(scenario)

        // Validate initial state
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))

        // Given - When we are located in the ReminderList Fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // When - We are in the Save Reminder Screen
        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title 1"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Description 1"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.btn_set_location_test)).perform(click())

        onView(withId(R.id.saveReminder)).perform(click())

        // Then - An error message should appear

        /*
        *  Solved an issue avoiding to display the toast message using this StackOverflow post:
        *  https://stackoverflow.com/questions/28390574/checking-toast-message-in-android
        * -espresso/28606603#28606603
        * */
        onView(withText(R.string.reminder_saved)).inRoot(
            withDecorView(
                not(
                    `is`(
                        getActivity(appContext)?.window?.decorView
                    )
                )
            )
        ).check(
            ViewAssertions.matches(
                isDisplayed()
            )
        )

        scenario.close()
    }

    @Test
    fun createReminder_cancelProcess_pressing_back() {
        // Set initial State
        val scenario = ActivityScenario.launch(RemindersActivity::class.java)

        // Setup the activity to monitor
        dataBindingIdlingResource.monitorActivity(scenario)

        // Validate initial state
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))

        // Given - When we are located in the ReminderList Fragment
        onView(withId(R.id.addReminderFAB)).perform(click())

        // When - We are in the Save Reminder Screen
        onView(withId(R.id.reminderTitle)).perform(ViewActions.replaceText("Title 1"))
        onView(withId(R.id.reminderDescription)).perform(ViewActions.replaceText("Description 1"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.btn_set_location_test)).perform(click())

        pressBack()

        // Then - We click back and the process is canceled
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(isDisplayed()))

        scenario.close()
    }
}
