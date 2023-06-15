package com.techun.demoemvttpax

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.ActivityCompat


class LocationFinder(context: Context) : Service(), LocationListener {

    var context: Context? = context

    var isGPSEnabled = false

    // flag for network status
    var isNetworkEnabled = false

    // flag for GPS status
    var canGetLocation = false
    var locationTemp: Location? = null
    var latitude = 0.0
    var longitude = 0.0

    // The minimum distance to change Updates in meters
    private  val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 10 // 10 meters

    // The minimum time between updates in milliseconds
    private val MIN_TIME_BW_UPDATES: Long

    // Declaring a Location Manager
    private var locationManager: LocationManager? = null

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onLocationChanged(p0: Location) {

    }

    fun getLocation(): Location? {
        try {
            locationManager = context?.getSystemService(LOCATION_SERVICE) as LocationManager
            // getting GPS status
            isGPSEnabled = locationManager!!
                .isProviderEnabled(LocationManager.GPS_PROVIDER)
            // getting network status
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
                // Log.e(“Network-GPS”, “Disable”);

              /*  if (!locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    // El proveedor de ubicación de red está desactivado, mostrar diálogo de solicitud de activación
                    val settingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context!!.startActivity(settingsIntent)
                }*/
            } else {
                canGetLocation = true
                // First get location from Network Provider
                if (isNetworkEnabled) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager!!.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                        )
                        // Log.e(“Network”, “Network”);
                        if (locationManager != null) {
                            locationTemp = locationManager!!
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (locationTemp != null) {
                                latitude = locationTemp!!.latitude
                                longitude = locationTemp!!.longitude
                            }
                        }
                    }

                } else  // if GPS Enabled get lat/long using GPS Services
                    if (isGPSEnabled) {
                        if (locationTemp == null) {
                            locationManager!!.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this
                            )
                            //Log.e(“GPS Enabled”, “GPS Enabled”);
                            if (locationManager != null) {
                                locationTemp = locationManager!!
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                if (locationTemp != null) {
                                    latitude = locationTemp!!.latitude
                                    longitude = locationTemp!!.longitude
                                }
                            }
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return locationTemp
    }

    fun getLatitud(): Double {
        if (locationTemp != null) {
            latitude = locationTemp!!.latitude
        }
        return latitude
    }

    fun getLongitud(): Double {
        if (locationTemp != null) {
            longitude = locationTemp!!.longitude
        }
        return longitude
    }

    fun canGetLocation(): Boolean {
        return canGetLocation
    }

    init {
        this.MIN_TIME_BW_UPDATES = (200 * 10 * 1).toLong()
        getLocation()
    }

}