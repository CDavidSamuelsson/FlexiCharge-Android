package com.flexicharge.bolt.activities

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.flexicharge.bolt.R
import com.flexicharge.bolt.api.flexicharge.RetrofitInstance
import com.flexicharge.bolt.databinding.ActivityProfileMenuLoggedInBinding
import com.flexicharge.bolt.helpers.LoginChecker

class ProfileMenuLoggedInActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileMenuLoggedInBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileMenuLoggedInBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.loginActivityButtonLogout.setOnClickListener {
            LoginChecker.LOGGED_IN = false
            getSharedPreferences("loginPreference", Context.MODE_PRIVATE).edit().apply {
                clear()
            }.apply()

            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}