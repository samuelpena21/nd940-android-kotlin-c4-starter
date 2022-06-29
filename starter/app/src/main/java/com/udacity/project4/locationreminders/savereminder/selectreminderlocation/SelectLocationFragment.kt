package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.IntentSender
import android.content.res.Resources
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
import androidx.annotation.VisibleForTesting
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.isLocationPermissionGranted
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.Locale

const val DEFAULT_ZOOM_LEVEL = 18f
const val DEFAULT_COORDINATE = 0.0
const val DEFAULT_OVERLAY_SIZE = 100f

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var marker: Marker? = null
    private var reminderDataItem: ReminderDataItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reminderDataItem = arguments?.getSerializable(REMINDER_DATA_ITEM_ARG) as? ReminderDataItem
        _viewModel.setReminderData(
            reminderDataItem ?: ReminderDataItem(
                "", "", "", 0.0, 0.0
            )
        )
    }

    private val locationPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
                permissionGranted ->
            if (!permissionGranted) {
                _viewModel.showSnackBar.value =
                    "Location permission is not granted. The my location button is disabled"
            } else {
                enableLocation()
            }
        }

    private val resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            enableLocation()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _viewModel.isLocationSelected.observe(viewLifecycleOwner) { isSelected ->
            binding.btnSetLocation.isEnabled = isSelected
        }

        binding.btnClear.setOnClickListener {
            removeMarker()
        }

        binding.btnSetLocation.setOnClickListener {
            onLocationSelected()
        }

        binding.btnSetLocationTest.setOnClickListener {
            onLocationSelectedTest()
        }
    }

    private fun onLocationSelected() {
        _viewModel.reminderData.value?.let {
            _viewModel.navigationCommand.value = NavigationCommand.To(
                SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment(
                    it
                )
            )
        }
    }

    @VisibleForTesting
    private fun onLocationSelectedTest() {
        val latLng = LatLng(13.45, 18.38)
        _viewModel.selectLocation(latLng, null)

        marker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(getString(R.string.dropped_pin))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )

        onLocationSelected()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        googleMap?.let {
            map = it
            enableLocation()
            zoomToSelectedLocation(googleMap)
            setMapOnLongClick(googleMap)
            setOnPoiClick(googleMap)
            setMapStyle(googleMap)
        }
    }

    private fun zoomToSelectedLocation(googleMap: GoogleMap) {
        reminderDataItem?.let {
            val latitude = it.latitude ?: DEFAULT_COORDINATE
            val longitude = it.longitude ?: DEFAULT_COORDINATE

            if (latitude != DEFAULT_COORDINATE || longitude != DEFAULT_COORDINATE) {
                val selectedLatLng = LatLng(latitude, longitude)
                marker = googleMap.addMarker(
                    MarkerOptions().position(selectedLatLng).title(getString(R.string.dropped_pin))
                )
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        selectedLatLng,
                        DEFAULT_ZOOM_LEVEL
                    )
                )
            }
        }
    }

    private fun setMapStyle(googleMap: GoogleMap) {
        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )

            if (!success) Log.e(TAG, "Style parsing failed")
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun setOnPoiClick(googleMap: GoogleMap) {
        googleMap.setOnPoiClickListener { poi ->
            removeMarker()
            val snippet = poi.name

            _viewModel.selectLocation(poi.latLng, poi.name)

            marker = googleMap.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
        }
    }

    private fun setMapOnLongClick(googleMap: GoogleMap) {
        googleMap.setOnMapLongClickListener { latLng ->
            removeMarker()
            val snippet = String.format(
                Locale.getDefault(),
                "Lat: %1$.5f, Long: %2$.5f",
                latLng.latitude,
                latLng.longitude
            )

            _viewModel.selectLocation(latLng, null)

            marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
            )
        }
    }

    private fun removeMarker() {
        marker?.remove()
        _viewModel.selectLocation(null, null)
    }

    @SuppressLint("MissingPermission")
    private fun enableLocation() {
        if (requireActivity().isLocationPermissionGranted()) {
            validateLocationIsEnabled {
                map.isMyLocationEnabled = true
            }
        } else {
            locationPermissionResult.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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

    companion object {
        val TAG: String = SelectLocationFragment::class.java.name
        const val REMINDER_DATA_ITEM_ARG = "reminderDataItem"
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 365
    }
}
