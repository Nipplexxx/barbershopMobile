package com.example.barbershopmob.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.barbershopmob.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class LoginViewModel : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    user?.let {
                        createUserIfNotExists(it.uid, it.email ?: "")
                        _loginResult.value = LoginResult(
                            success = LoggedInUserView(displayName = it.email ?: "Пользователь")
                        )
                    }
                } else {
                    // Если не удалось войти — пробуем зарегистрировать
                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { createTask ->
                            if (createTask.isSuccessful) {
                                val user = firebaseAuth.currentUser
                                user?.let {
                                    createUserIfNotExists(it.uid, it.email ?: "")
                                    _loginResult.value = LoginResult(
                                        success = LoggedInUserView(displayName = it.email ?: "Пользователь")
                                    )
                                }
                            } else {
                                _loginResult.value = LoginResult(error = R.string.login_failed)
                            }
                        }
                }
            }
    }

    private fun createUserIfNotExists(uid: String, email: String) {
        val userRef = database.child("users").child(uid)
        userRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                val newUserData = mapOf(
                    "email" to email,
                    "fullName" to "",
                    "points" to 0,
                    "appointments" to emptyMap<String, Any>() // Или пустой список
                )
                userRef.setValue(newUserData)
            }
        }
    }

    fun loginDataChanged(username: String, password: String) {
        if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isUserNameValid(username: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length >= 6
    }
}