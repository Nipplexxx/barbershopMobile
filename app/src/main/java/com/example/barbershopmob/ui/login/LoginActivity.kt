package com.example.barbershopmob.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.barbershopmob.MainActivity
import com.example.barbershopmob.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val isFromLogout = intent.getBooleanExtra("fromLogout", false)
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null && !isFromLogout) {
            // Пользователь авторизован — проверяем роль
            checkUserRole(currentUser.uid)
        } else {
            // Отображаем экран логина
            setContentView(R.layout.activity_login)

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.login_fragment_container, LoginFragment())
                    .commit()
            }
        }
    }

    private fun checkUserRole(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val userMap = mapOf(
                    "role" to "client",
                    "email" to email,
                    "points" to 0 // Инициализация баллов
                )
                userRef.setValue(userMap).addOnCompleteListener {
                    goToMain("client", 0)
                }
            } else {
                val role = snapshot.child("role").getValue(String::class.java) ?: "client"
                val points = snapshot.child("points").getValue(Int::class.java) ?: 0

                if (!snapshot.hasChild("role")) {
                    userRef.child("role").setValue("client")
                }
                if (!snapshot.hasChild("points")) {
                    userRef.child("points").setValue(0)
                }
                goToMain(role, points)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Ошибка при получении данных", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()
            recreate()
        }
    }

    private fun goToMain(role: String, points: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("userRole", role)
            putExtra("userPoints", points)
        }
        startActivity(intent)
        finish()
    }

    private fun goToMain(role: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("userRole", role)
        }
        startActivity(intent)
        finish()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}