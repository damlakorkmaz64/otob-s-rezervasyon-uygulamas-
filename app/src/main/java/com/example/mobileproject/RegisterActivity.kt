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

class RegisterActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    // Emülatör için:
    private val BASE_URL = "http://10.0.2.2:8000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etPasswordAgain = findViewById<EditText>(R.id.etPasswordAgain)

        val rbUser = findViewById<RadioButton>(R.id.rbUser)
        val rbAdmin = findViewById<RadioButton>(R.id.rbAdmin)

        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordAgain = etPasswordAgain.text.toString().trim()
            val role = if (rbAdmin.isChecked) "admin" else "user"

            if (username.isEmpty() || password.isEmpty() || passwordAgain.isEmpty()) {
                Toast.makeText(this, "Boş alan bırakmayın", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordAgain) {
                Toast.makeText(this, "Şifreler uyuşmuyor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Backend register
            registerWithBackend(username, password, role)
        }
    }

    private fun registerWithBackend(username: String, password: String, role: String) {
        thread {
            try {
                val url = URL("$BASE_URL/auth/register")
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
                    put("role", role)
                }

                conn.outputStream.use { os ->
                    os.write(json.toString().toByteArray(Charsets.UTF_8))
                }

                val code = conn.responseCode
                val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    .bufferedReader()
                    .readText()

                runOnUiThread {
                    if (code in 200..299) {
                        Toast.makeText(this, "Kayıt başarılı ✅", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        // 409: username alınmış
                        Toast.makeText(this, "Kayıt başarısız: $body", Toast.LENGTH_LONG).show()
                        Log.e("REGISTER_API", "HTTP $code body=$body")
                    }
                }

                conn.disconnect()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Register FAIL: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("REGISTER_API", "Exception", e)
                }
            }
        }
    }
}

