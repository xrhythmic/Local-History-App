package com.xrhythmic.localhistoryapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.firestore.FirebaseFirestore
import com.xrhythmic.localhistoryapp.FirebaseUtils
import com.xrhythmic.localhistoryapp.MainActivity
import com.xrhythmic.localhistoryapp.R
import java.util.*


class MapFragment : Fragment() {
    private val viewModel: DataViewModel by viewModels()
    lateinit var database: FirebaseFirestore

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentAddress: Address? = null

    private var cancellationTokenSource: CancellationTokenSource? = null
    private var cancellationToken: CancellationToken? = null

    var localPois: ArrayList<MutableMap<String, Any>>? =
        ArrayList<MutableMap<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        database = FirebaseFirestore.getInstance()
        val root = inflater.inflate(R.layout.fragment_map, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val parent = (activity as MainActivity?)

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?  //use SupportMapFragment for using in fragment instead of activity  MapFragment = activity   SupportMapFragment = fragment


        mapFragment!!.getMapAsync { mMap ->
            if (ActivityCompat.checkSelfPermission(
                    container!!.context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    container.context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                mMap.isMyLocationEnabled = true
            }

            mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            mMap.clear() //clear old markers


            mMap.setOnMapClickListener(OnMapClickListener { point ->
                val marker = MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("New Marker")

                val geocoder = Geocoder(parent, Locale.getDefault())
                val address: Address =
                    geocoder.getFromLocation((point.latitude), (point.longitude), 1)[0]


                val poiData = hashMapOf<String, Any>(
                    "admin" to address.adminArea,
                    "sub-admin" to address.subAdminArea,
                    "description" to "Example Description",
                    "images" to  ArrayList<Any>().add("https://miro.medium.com/max/640/0*DSmHXQ2-F3FMpevO.jpg"),
                    "location" to LatLng(point.latitude, point.longitude),
                    "name" to marker.title
                )

                FirebaseUtils().fireStoreDatabase.collection("pois").document("(${point.latitude})-(${point.longitude})")
                    .set(poiData)
                    .addOnSuccessListener {
                        Log.d("Data Added Successfully", "Added document")
                    }
                    .addOnFailureListener { exception ->
                        Log.w("Data Failed to be added", "Error adding document $exception")
                    }

                mMap.addMarker(marker)
                Log.d("MARKER ADDED", point.latitude.toString() + "---" + point.longitude)
            })
        }

        viewModel.address.observe(viewLifecycleOwner, Observer<Address> {
            Log.d("ADDRESS UPDATES", "ADDRESS UPDATED")

        })

        getPois()

        return root
    }

    private fun getPois() {
        Log.d("Location", viewModel.address.value.toString())
        val poisQuery = database.collection("pois")
        // Get the documents
        poisQuery.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    localPois?.add(document.data  as MutableMap<String, Any>)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("LOCATION", "Error getting documents: ", exception)
            }
            .addOnCompleteListener {
                if (localPois.isNullOrEmpty()) {
                    getPoisByAdmin()
                } else {
                    updateMap()
                }
            }
    }

    private fun getPoisByAdmin() {
        val poisQuery = database.collection("pois").whereEqualTo("admin",
            viewModel.address.value?.adminArea
        )

        Log.d("LOCATION", "Get POIs")
        // Get the documents
        poisQuery.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    localPois?.add(document.data  as MutableMap<String, Any>)
                }
            }
            .addOnFailureListener { exception ->
                Log.w("LOCATION", "Error getting documents: ", exception)
            }
    }

    private fun updateMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        Log.d("LOCATION", "Updating LOCATION")
        mapFragment!!.getMapAsync { mMap ->
            for (poi in localPois!!) {
                Log.d("LOCATION", poi.toString())
                val location = poi["location"] as HashMap<*, *>
                val marker = MarkerOptions()
                    .position(LatLng(location["latitude"] as Double, location["longitude"] as Double))
                    .title(poi["name"] as String?)

                mMap.addMarker(marker)
            }
        }
    }

    private fun bitmapDescriptorFromVector(context: Context?, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(requireContext(), vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap =
            Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermissions()
        }
        else {
            getCurrentLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        cancellationTokenSource = CancellationTokenSource()
        cancellationToken = cancellationTokenSource!!.token

        fusedLocationClient?.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, cancellationToken!!)
            ?.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful && task.result != null) {
                    Log.d(TAG, "getLastLocation Succeeded ${task.result}")

                    val geocoder = Geocoder(requireActivity(), Locale.getDefault())
                    val addresses: List<Address> = geocoder.getFromLocation((task.result)!!.latitude, (task.result)!!.longitude, 1)
                    currentAddress = addresses[0]
                    moveMap()
                }
                else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    showMessage("No location detected. Make sure location is enabled on the device.")
                }
            }
    }
    private fun moveMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?

        mapFragment!!.getMapAsync { mMap ->
            val loc = currentAddress?.let { LatLng(it.latitude, it.longitude) }
            val marker = loc?.let {
                MarkerOptions()
                    .position(it)
                    .title("You're Here!")
            }

            mMap.addMarker(marker)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
        }
    }
    private fun showMessage(string: String) {
        Toast.makeText(requireActivity(), string, Toast.LENGTH_LONG).show()
    }
    private fun showSnackbar(
        mainTextStringId: String, actionStringId: String,
        listener: View.OnClickListener
    ) {
        Toast.makeText(requireActivity(), mainTextStringId, Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions(): Boolean {
        val coarsePermissionState = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        val finePermissionState = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        val backgroundPermissionState = ActivityCompat.checkSelfPermission(
            requireActivity(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
        )

        val allPermissionsEqual = (backgroundPermissionState == finePermissionState && coarsePermissionState == backgroundPermissionState )

        return (coarsePermissionState == PackageManager.PERMISSION_GRANTED) && allPermissionsEqual
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldCoarseProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val shouldFineProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val shouldBackgroundProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            requireActivity(),
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        if (shouldCoarseProvideRationale || shouldFineProvideRationale || shouldBackgroundProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            showSnackbar("Location permission is needed for core functionality", "Okay",
                View.OnClickListener {
                    startLocationPermissionRequest()
                })
        }
        else {
            Log.i(TAG, "Requesting permission")
            startLocationPermissionRequest()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            when {
                grantResults.isEmpty() -> {
                    // If user interaction was interrupted, the permission request is cancelled and you
                    // receive empty arrays.
                    Log.i(TAG, "User interaction was cancelled.")
                }
                grantResults[0] == PackageManager.PERMISSION_GRANTED -> {
                    // Permission granted.
                    getCurrentLocation()
                }
                else -> {
                    showSnackbar("Permission was denied", "Settings",
                        View.OnClickListener {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                Build.DISPLAY, null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }


    companion object {
        private val TAG = "LocationProvider"
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}