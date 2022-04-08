package com.xrhythmic.localhistoryapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
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
import com.xrhythmic.localhistoryapp.AddPoiActivity
import com.xrhythmic.localhistoryapp.MainActivity
import com.xrhythmic.localhistoryapp.R
import com.xrhythmic.localhistoryapp.ShowPoiActivity
import java.util.*


class MapFragment : Fragment() {
    private val viewModel: DataViewModel by viewModels()
    lateinit var database: FirebaseFirestore

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var currentAddress: Address? = null
    var user: MutableMap<String, Any> = mutableMapOf<String, Any>()

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
        getUser(parent?.intent?.getStringExtra("email").toString())

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
                if (user["role"]?.equals("admin") == true) {
                    val intent = Intent(activity, AddPoiActivity::class.java)
                    intent.putExtra("latitude", point.latitude)
                    intent.putExtra("longitude", point.longitude)
                    startActivity(intent)
                }
            })

            mMap.setOnMarkerClickListener { marker ->
                if (marker.title != "You're Here!") {
                    val pos = marker.position
                    val intent = Intent(activity, ShowPoiActivity::class.java)
                    intent.putExtra("id", "(${pos.latitude})-(${pos.longitude})")
                    intent.putExtra("userRole", "${user["role"]}")
                    startActivity(intent)
                }
                true
            }
        }
        return root
    }

    private fun getPois() {
        val poisQuery = database.collection("pois").whereEqualTo("sub-admin",
            currentAddress?.subAdminArea
        )
        // Get the documents
        localPois = ArrayList<MutableMap<String, Any>>()
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
            currentAddress?.adminArea
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
            .addOnCompleteListener {
            if (localPois.isNullOrEmpty()) {
                showMessage("No POIs found in your area")
                updateMap()
            } else {
                updateMap()
            }
        }
    }

    private fun updateMap() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.frg) as SupportMapFragment?
        Log.d("LOCATION", "Updating LOCATION")
        mapFragment!!.getMapAsync { mMap ->
            mMap.clear()
            val loc = currentAddress?.let { LatLng(it.latitude, it.longitude) }
            val marker = loc?.let {
                MarkerOptions()
                    .position(it)
                    .title("You're Here!")
            }
            mMap.addMarker(marker).showInfoWindow()
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 15f));
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
                    getPois()
                }
                else {
                    Log.w(TAG, "getLastLocation:exception", task.exception)
                    showMessage("No location detected. Make sure location is enabled on the device.")
                }
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

        val allPermissionsEqual = ( finePermissionState == coarsePermissionState )

        return (coarsePermissionState == PackageManager.PERMISSION_GRANTED) && allPermissionsEqual
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION),
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
        if (shouldCoarseProvideRationale || shouldFineProvideRationale ) {
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

    private fun getUser(email: String) {
        val docRef = database.collection("users").document(email)

        // Get the document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user = task.result.data as MutableMap<String, Any>
                viewModel.setCurrentUser(user)

                Log.d("COMPLETE", "Document get succeeded: ${viewModel.user.value}")
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }


    companion object {
        private val TAG = "LocationProvider"
        private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
    }
}