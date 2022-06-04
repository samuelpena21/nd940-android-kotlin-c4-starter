package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.isLocationPermissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit

@SuppressLint("UnspecifiedImmutableFlag")
class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
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
            } else {
                _viewModel.showToast.value =
                    "You need to grant permissions to be able to save the Location"
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        reminderDataItem = arguments?.getSerializable(REMINDER_DATA_ITEM_ARG) as? ReminderDataItem ?: ReminderDataItem(
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
                NavigationCommand.To(SaveReminderFragmentDirections
                    .actionSaveReminderFragmentToSelectLocationFragment(reminderDataItem))
        }

        binding.saveReminder.setOnClickListener {
            val reminderDataItem = _viewModel.reminderData.value
            reminderDataItem?.let {
                addGeofenceForClue(it)
            }
        }
    }

    private fun showGrantPermissionDialog() {
        val alertDialog: AlertDialog = requireActivity().let {
            val builder = AlertDialog.Builder(it)
            builder.apply {
                setMessage("You need to grant permissions to be able to save the Location")
                setPositiveButton(
                    "OK"
                ) { _, _ ->
                    resultCallback.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }

            // Create the AlertDialog
            builder.create()
        }
        alertDialog.show()
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
                if (requireActivity().isLocationPermissionGranted()) {
                    if (_viewModel.validateEnteredData(reminderDataItem)) {
                        addGeoFence(reminderDataItem, geofencingRequest, geoFencePendingIntent)
                    }
                } else {
                    showGrantPermissionDialog()
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
                _viewModel.delete()
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
