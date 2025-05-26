package com.example.barbershopmob.ui.barbers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BarbersViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Наши барберы — профессионалы своего дела"
    }
    val text: LiveData<String> = _text
}