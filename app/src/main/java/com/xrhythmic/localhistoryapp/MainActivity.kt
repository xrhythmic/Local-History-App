package com.xrhythmic.localhistoryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import com.xrhythmic.localhistoryapp.databinding.ActivityMainBinding

private lateinit var binding: ActivityMainBinding

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userId = intent.getStringExtra("user_id")
        val emailId = intent.getStringExtra("email_id")

        binding.tvUserId.text = "User ID :: $userId"
        binding.tvEmailId.text = "Email :: $emailId"

        binding.btnLogout.setOnClickListener {
            // Logging out from app
            FirebaseAuth.getInstance().signOut()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }
}