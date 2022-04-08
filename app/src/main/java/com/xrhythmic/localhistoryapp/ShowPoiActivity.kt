package com.xrhythmic.localhistoryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.squareup.picasso.Picasso
import com.xrhythmic.localhistoryapp.databinding.ActivityAddPoiBinding
import com.xrhythmic.localhistoryapp.databinding.ActivityShowPoiBinding
import java.util.*

class ShowPoiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityShowPoiBinding
    var poi: MutableMap<String, Any> = mutableMapOf<String, Any>()
    lateinit var userRole: String
    lateinit var mTTS: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        userRole = intent.getStringExtra("userRole").toString()

        if (userRole == "admin") {
           binding.btnAdminDelete.visibility = View.VISIBLE
           binding.btnAdminEdit.visibility = View.VISIBLE
        }


        mTTS = TextToSpeech(this, TextToSpeech.OnInitListener { status ->
            if (status != TextToSpeech.ERROR) {
                //if no error then set lang
                mTTS.language = Locale.UK
            }

            binding.btnSpeakPoi.setOnClickListener {
                if(mTTS.isSpeaking) {
                    mTTS.stop()
                    speakPoi()
                } else {
                    speakPoi()
                }
            }

        })

    }

    override fun onStart() {
        super.onStart()
        getPoi(intent.getStringExtra("id").toString())
    }

    private fun getPoi(id: String) {
        val docRef = FirebaseUtils().fireStoreDatabase.collection("pois").document(id)

        // Get the document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("POI ERROR", task.result.data.toString())
                poi = task.result.data as MutableMap<String, Any>
                binding.tvTitle.text = poi["name"].toString()
                binding.tvDescription.text = poi["description"].toString()
                val firstImage = poi["image"].toString()
                if (firstImage.isNotBlank()) {
                    Picasso.get().load(firstImage).into(binding.ivPoi)
                }
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }

    fun speakPoi() {
        val poiTextToSpeak = "Point of interest name: ${binding.tvTitle.text}. Description: ${binding.tvDescription.text}"
        mTTS.speak(poiTextToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun deletePoi(view: View) {
        val docRef = FirebaseUtils().fireStoreDatabase.collection("pois").document(intent.getStringExtra("id").toString())

        docRef.delete()
            .addOnSuccessListener {
                Log.d("DocDelete", "DocumentSnapshot successfully deleted!")
                finish()
            }
            .addOnFailureListener { e ->
                Log.w("DocDelete", "Error deleting document", e)
            }
    }

    fun editPoi(view: View) {
        val id = intent.getStringExtra("id").toString()
        val intent = Intent(this, AddPoiActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

}