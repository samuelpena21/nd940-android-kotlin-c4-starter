package com.udacity.project4.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

const val REQUEST_LOCATION_PERMISSION: Int = 111

fun Activity.isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
    this,
    Manifest.permission.ACCESS_FINE_LOCATION
) == PackageManager.PERMISSION_GRANTED