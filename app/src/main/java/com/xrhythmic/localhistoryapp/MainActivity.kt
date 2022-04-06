package com.xrhythmic.localhistoryapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.xrhythmic.localhistoryapp.databinding.ActivityMainBinding
import java.util.*
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null

    private var latitude by Delegates.notNull<Double>()
    private var longitude by Delegates.notNull<Double>()
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var user: MutableMap<String, Any>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        database = FirebaseFirestore.getInstance()

        val email = intent.getStringExtra("email").toString()
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)



        getUser(email)

        navView.setupWithNavController(navController)

        mainBinding.addPoi.visibility = View.INVISIBLE

        mainBinding.addPoi.setOnClickListener {
            if (user["role"]?.equals("admin") == true) {
                Toast.makeText(this, "Clicked Add POI", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "You are not an Admin", Toast.LENGTH_SHORT).show()
            }
        }

        mainBinding.setting.setOnClickListener {
            Toast.makeText(this, "Clicked Settings", Toast.LENGTH_SHORT).show()
        }

        mainBinding.logOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun getUser(email: String) {
        val docRef = database.collection("users").document(email)

        // Get the document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                user = task.result.data as MutableMap<String, Any>
                Log.d("COMPLETE", "Document get succeeded: $user")
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions()
            }
        }
        else {
            getLastLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation() {
        fusedLocationClient?.lastLocation!!.addOnCompleteListener(this) { task ->
            if (task.isSuccessful && task.result != null) {
                Log.d(TAG, "getLastLocation Succeeded ${task.result}", )
                lastLocation = task.result

                val geocoder = Geocoder(this, Locale.getDefault())
                Log.d("Location", (lastLocation)?.latitude.toString())
                Log.d("Location", (lastLocation)?.longitude.toString())
                val addresses: List<Address> = geocoder.getFromLocation((lastLocation)!!.latitude, (lastLocation)!!.longitude, 1)
                val currentCityName: String = addresses[0].getAddressLine(0)
                Log.d("Location", currentCityName)
            }
            else {
                Log.w(TAG, "getLastLocation:exception", task.exception)
                showMessage("No location detected. Make sure location is enabled on the device.")
            }
        }
    }
    private fun showMessage(string: String) {
        val container = findViewById<View>(R.id.layout)
        if (container != null) {
            Toast.makeText(this@MainActivity, string, Toast.LENGTH_LONG).show()
        }
    }
    private fun showSnackbar(
        mainTextStringId: String, actionStringId: String,
        listener: View.OnClickListener
    ) {
        Toast.makeText(this@MainActivity, mainTextStringId, Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun startLocationPermissionRequest() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            REQUEST_PERMISSIONS_REQUEST_CODE
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (shouldProvideRationale) {
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
                    getLastLocation()
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