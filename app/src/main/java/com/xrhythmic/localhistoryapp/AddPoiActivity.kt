package com.xrhythmic.localhistoryapp

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.xrhythmic.localhistoryapp.databinding.ActivityAddPoiBinding
import java.util.*

class AddPoiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPoiBinding
    private lateinit var latitude: String
    private lateinit var longitude: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        latitude = intent.getDoubleExtra("latitude", 0.0).toString()
        longitude = intent.getDoubleExtra("longitude", 0.0).toString()

        binding.tvLatitude.text = latitude
        binding.tvLongitude.text = longitude
    }

    fun addPoi(view: View) {
        when {
            TextUtils.isEmpty(binding.etPoiName.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this,
                    "Please enter a name",
                    Toast.LENGTH_SHORT
                ).show()
            }

            TextUtils.isEmpty(
                binding.etPoiDescription.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this,
                    "Please enter a description",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                val name: String = binding.etPoiDescription.text.toString()
                val description: String = binding.etPoiDescription.text.toString()

                val geocoder = Geocoder(this, Locale.getDefault())
                val address: Address =
                    geocoder.getFromLocation((latitude.toDouble()), (longitude.toDouble()), 1)[0]
                val images = ArrayList<Any>()
                images.add("https://miro.medium.com/max/640/0*DSmHXQ2-F3FMpevO.jpg")

                val poiData = hashMapOf<String, Any>(
                    "admin" to address.adminArea,
                    "sub-admin" to address.subAdminArea,
                    "description" to description,
                    "images" to  images,
                    "location" to LatLng(latitude.toDouble(), longitude.toDouble()),
                    "name" to name
                )

                Log.d("POI", "$poiData")

                FirebaseUtils().fireStoreDatabase.collection("pois").document("($latitude)-($longitude})")
                    .set(poiData)
                    .addOnSuccessListener {
                        Log.d("POI", "Added document")
                    }
                    .addOnFailureListener { exception ->
                        Log.w("POI", "Error adding document $exception")
                    }
                    .addOnCompleteListener {
                        finish()
                    }

            }
        }
    }
}