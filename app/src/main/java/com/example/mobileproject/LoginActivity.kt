package com.example.mobileproject

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class LoginActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // Emülatör için:
    private val BASE_URL = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // 🔁 Daha önce giriş yapılmış mı?
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)
        val savedRole = prefs.getString("role", null)

        if (isLoggedIn && savedRole != null) {
            goByRole(savedRole)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnGoRegister)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Boş alan bırakmayın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Backend login
            loginWithBackend(username, password)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginWithBackend(username: String, password: String) {
        thread {
            try {
                val url = URL("$BASE_URL/auth/login")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                }

                val json = JSONObject().apply {
                    put("username", username)
                    put("password", password)
                }

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader()
                    .readText()

                if (code in 200..299) {
                    // response: { "id": 1, "username": "...", "role": "user/admin" }
                    val obj = JSONObject(body)
                    val userId = obj.getInt("id")
                    val role = obj.getString("role")

                    runOnUiThread {
                        saveSession(userId, username, role)
                        Toast.makeText(this, "Giriş başarılı ✅", Toast.LENGTH_SHORT).show()
                        goByRole(role)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this, "Giriş başarısız: $body", Toast.LENGTH_LONG).show()
                        Log.e("LOGIN_API", "HTTP $code body=$body")
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Login FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("LOGIN_API", "Exception", e)
                }
            }
        }
    }

    private fun saveSession(userId: Int, username: String, role: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putInt("user_id", userId)
            .putString("username", username)
            .putString("role", role)
            .apply()
    }

    private fun goByRole(role: String) {
        if (role == "admin") {
            startActivity(Intent(this, AdminActivity::class.java))
        } else {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}


