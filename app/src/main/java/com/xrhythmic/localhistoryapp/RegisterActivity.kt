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
import com.xrhythmic.localhistoryapp.databinding.ActivityRegisterBinding

private lateinit var binding: ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }

    fun register(view: View) {
        when {
            TextUtils.isEmpty(binding.etRegisterEmail.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@RegisterActivity,
                    "Please enter an email address.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            TextUtils.isEmpty(
                binding.etRegisterPassword.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@RegisterActivity,
                    "Please enter a password",
                    Toast.LENGTH_SHORT
                ).show()
            }

            TextUtils.isEmpty(
                binding.etRegisterPasswordConfirmation.text.toString().trim { it <= ' ' }) -> {
                Toast.makeText(
                    this@RegisterActivity,
                    "Please enter a password confirmation",
                    Toast.LENGTH_SHORT
                ).show()
            }

            binding.etRegisterPasswordConfirmation.text.toString().trim { it <= ' ' }
                    != binding.etRegisterPassword.text.toString().trim { it <= ' ' } -> {
                Toast.makeText(
                    this@RegisterActivity,
                    "Passwords do not match!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                val email: String = binding.etRegisterEmail.text.toString().trim { it <= ' '}
                val password: String = binding.etRegisterPassword.text.toString().trim { it <= ' '}

                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
                    .addOnCompleteListener(
                        OnCompleteListener<AuthResult> { task ->

                            if (task.isSuccessful) {
                                val firebaseUser: FirebaseUser = task.result!!.user!!

                                Toast.makeText(
                                    this@RegisterActivity,
                                    "You have successfully registered for an account!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                /**
                                 * Here the user is registered and automatically signed in so I have
                                 * to send them to the main screen
                                 */

                                val intent =
                                    Intent(this@RegisterActivity, MainActivity::class.java)

                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                intent.putExtra("user_id", firebaseUser.uid)
                                intent.putExtra("email_id", email)
                                startActivity(intent)
                                finish()
                            } else {
                                Toast.makeText(
                                    this@RegisterActivity,
                                    task.exception!!.message.toString(),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
            }
        }
    }
}