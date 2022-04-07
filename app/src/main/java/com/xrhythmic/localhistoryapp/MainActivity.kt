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
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.xrhythmic.localhistoryapp.databinding.ActivityMainBinding
import com.xrhythmic.localhistoryapp.ui.map.DataViewModel
import java.util.*


class MainActivity : AppCompatActivity() {
    private val viewModel: DataViewModel by viewModels()

    private lateinit var mainBinding: ActivityMainBinding
    lateinit var database: FirebaseFirestore
    var user: MutableMap<String, Any> = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        database = FirebaseFirestore.getInstance()

        val email = intent.getStringExtra("email").toString()
        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)

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
                viewModel.setCurrentUser(user)
                Log.d("COMPLETE", "Document get succeeded: $user")
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }
}