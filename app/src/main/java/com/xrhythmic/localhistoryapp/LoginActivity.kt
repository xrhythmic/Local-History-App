package com.xrhythmic.localhistoryapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.xrhythmic.localhistoryapp.databinding.ActivityLoginBinding

private lateinit var binding: ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    fun login(view: View) {
        when {
            TextUtils.isEmpty(binding.etLoginEmail.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    "Please enter an email address.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            TextUtils.isEmpty(
                binding.etLoginPassword.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@LoginActivity,
                    "Please enter a password",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                val email: String = binding.etLoginEmail.text.toString().trim { it <= ' '}
                val password: String = binding.etLoginPassword.text.toString().trim { it <= ' '}

                FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(
                        OnCompleteListener<AuthResult> { task ->
                            if (task.isSuccessful) {
                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                Toast.makeText(
                                    this@LoginActivity,
                                    "You have successfully Logged In!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                FirebaseUtils().fireStoreDatabase.collection("users").document(email)
                                    .get()




                                /**
                                 * Here the user is logged in so I have to send them to the main screen
                                 */
                                val intent =
                                    Intent(this@LoginActivity, MainActivity::class.java)

                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("email", email)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@LoginActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
            }
        }
    }

    fun register(view: View) {
        val intent =
            Intent(this@LoginActivity, RegisterActivity::class.java)

        startActivity(intent)
        finish()
    }
}