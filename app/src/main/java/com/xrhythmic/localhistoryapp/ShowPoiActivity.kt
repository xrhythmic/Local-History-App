package com.xrhythmic.localhistoryapp

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
    lateinit var mTTS: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowPoiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getPoi(intent.getStringExtra("id").toString())



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

    private fun getPoi(id: String) {
        val docRef = FirebaseUtils().fireStoreDatabase.collection("pois").document(id)

        // Get the document
        docRef.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                poi = task.result.data as MutableMap<String, Any>
                binding.tvTitle.text = poi["name"].toString()
                binding.tvDescription.text = poi["description"].toString()
                val firstImage = (poi["images"] as ArrayList<*>)[0].toString()
                Picasso.get().load(firstImage).into(binding.ivPoi)
            } else {
                Log.d("ERROR", "Document get failed: ", task.exception)
            }
        }
    }

    fun speakPoi() {
        val poiTextToSpeak = "Point of interest name: ${binding.tvTitle.text}. Description: ${binding.tvDescription.text}"
        mTTS.speak(poiTextToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
    }
}