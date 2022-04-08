package com.xrhythmic.localhistoryapp

import android.app.ProgressDialog
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.net.toUri
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.xrhythmic.localhistoryapp.databinding.ActivityAddPoiBinding
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class AddPoiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddPoiBinding
    private lateinit var storage: FirebaseStorage
    private lateinit var ImageUri: Uri
    private lateinit var OriginalImage: String
    private lateinit var latitude: String
    private lateinit var longitude: String
    var poi: MutableMap<String, Any> = mutableMapOf<String, Any>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPoiBinding.inflate(layoutInflater)
        storage = Firebase.storage("gs://poi_images")
        setContentView(binding.root)

        if (intent.getStringExtra("id").isNullOrBlank()) {
            latitude = intent.getDoubleExtra("latitude", 0.0).toString()
            longitude = intent.getDoubleExtra("longitude", 0.0).toString()
            binding.tvLatitude.text = latitude
            binding.tvLongitude.text = longitude
            binding.tvTitle.text = "Add a new POI"
        } else {
            setPoi(intent.getStringExtra("id").toString())
            binding.tvTitle.text = "Edit an existing POI"
            binding.btnAddPoi.text = "Edit POI"
        }

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
                binding.ivPoi.setImageURI(null)

                val name: String = binding.etPoiName.text.toString()
                val description: String = binding.etPoiDescription.text.toString()
                val image: String = binding.etPoiImage.text.toString()

                val geocoder = Geocoder(this, Locale.getDefault())
                val address: Address =
                    geocoder.getFromLocation((binding.tvLatitude.text.toString().toDouble()), (binding.tvLongitude.text.toString().toDouble()), 1)[0]

                val poiData = hashMapOf<String, Any>(
                    "admin" to address.adminArea,
                    "sub-admin" to address.subAdminArea,
                    "description" to description,
                    "image" to  image,
                    "location" to LatLng(binding.tvLatitude.text.toString().toDouble(), binding.tvLongitude.text.toString().toDouble()),
                    "name" to name
                )
                Log.d("POI", "UPDATING/CREATING")

                Log.d("POI", "$poiData")

                FirebaseUtils().fireStoreDatabase.collection("pois").document("(${binding.tvLatitude.text})-(${binding.tvLongitude.text})")
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

    private fun setPoi(id: String) {
        val docRef = FirebaseUtils().fireStoreDatabase.collection("pois").document(id)
        // Get the document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                poi = task.result.data as MutableMap<String, Any>
                val location = poi["location"] as HashMap<*, *>
                OriginalImage = poi["image"].toString()
                binding.tvLatitude.text = location["latitude"].toString()
                binding.tvLongitude.text = location["longitude"].toString()
                binding.etPoiName.setText(poi["name"].toString())
                binding.etPoiDescription.setText(poi["description"].toString())
                binding.etPoiImage.setText(poi["image"].toString())

                if(poi["image"].toString().isNotBlank()) {
                    Picasso.get().load(poi["image"].toString()).into(binding.ivPoi)
                }
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }
}