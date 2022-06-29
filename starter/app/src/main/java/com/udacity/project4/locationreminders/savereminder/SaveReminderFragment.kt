package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.isLocationPermissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.concurrent.TimeUnit

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    private val runningQOrLater: Boolean =
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var reminderDataItem: ReminderDataItem

    private val geoFencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireActivity(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private val resultCallback =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) {
                _viewModel.showToast.value = "Permission granted!"
                _viewModel.reminderData.value?.let { item ->
                    addGeofenceForClue(item)
                }
            } else {
                _viewModel.showToast.value =
                    "You need to grant permissions to be able to save the Location"
            }
        }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            _viewModel.reminderData.value?.let {
                addGeofenceForClue(it)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        reminderDataItem = arguments?.getSerializable(REMINDER_DATA_ITEM_ARG) as? ReminderDataItem
            ?: ReminderDataItem(
                "", "", "", 0.0, 0.0
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)
        setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        buildUiWithItem(reminderDataItem)
        initListeners()
    }

    private fun buildUiWithItem(reminderDataItem: ReminderDataItem) {
        _viewModel.setReminderData(reminderDataItem)
    }

    private fun initListeners() {
        binding.selectLocation.setOnClickListener {
            _viewModel.navigationCommand.value =
                NavigationCommand.To(
                    SaveReminderFragmentDirections
                        .actionSaveReminderFragmentToSelectLocationFragment(reminderDataItem)
                )
        }

        binding.saveReminder.setOnClickListener {
            val reminderDataItem = _viewModel.reminderData.value
            reminderDataItem?.let {
                addGeofenceForClue(it)
            }
        }
    }

    private fun addGeofenceForClue(reminderDataItem: ReminderDataItem) {
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude ?: 0.0,
                reminderDataItem.longitude ?: 0.0,
                100f
            )
            .setExpirationDuration(TimeUnit.HOURS.toMillis(1))
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.removeGeofences(geoFencePendingIntent)?.run {
            addOnCompleteListener {
                if (_viewModel.validateEnteredData(reminderDataItem)) {
                    if (!requireActivity().isLocationPermissionGranted()) {
                        resultCallback.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        return@addOnCompleteListener
                    }

                    if (!foregroundAndBackgroundLocationPermissionApproved()) {
                        resultCallback.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        return@addOnCompleteListener
                    }

                    validateLocationIsEnabled {
                        addGeoFence(reminderDataItem, geofencingRequest, geoFencePendingIntent)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeoFence(
        reminderDataItem: ReminderDataItem,
        geofencingRequest: GeofencingRequest,
        geoFencePendingIntent: PendingIntent
    ) {
        geofencingClient.addGeofences(geofencingRequest, geoFencePendingIntent)?.run {
            addOnSuccessListener {
                _viewModel.validateAndSaveReminder(reminderDataItem)
            }
            addOnFailureListener {
                val message = it.message
                _viewModel.onFailure(message.orEmpty())
            }
        }
    }

    private fun validateLocationIsEnabled(onLocationEnabled: () -> Unit) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingClient = LocationServices.getSettingsClient(requireContext())
        val responseTask = settingClient.checkLocationSettings(builder.build())
        responseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest =
                        IntentSenderRequest.Builder(exception.resolution).build()
                    resultLauncher.launch(intentSenderRequest)
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Location request exception: ${e.message}")
                }
            } else {
                _viewModel.showSnackBar.value = "You must enable location"
            }
        }

        responseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                onLocationEnabled()
            }
        }
    }

    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
            PackageManager.PERMISSION_GRANTED ==
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.delete_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete -> {
                deleteItem()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteItem() {
        geofencingClient.removeGeofences(geoFencePendingIntent)?.run {
            addOnCompleteListener {
                _viewModel.reminderData.value?.let {
                    _viewModel.delete(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        val TAG = SaveReminderFragment::class.java.name
        internal const val ACTION_GEOFENCE_EVENT =
            "HuntMainActivity.treasureHunt.action.ACTION_GEOFENCE_EVENT"
        const val REMINDER_DATA_ITEM_ARG = "reminderDataItem"
    }
}
