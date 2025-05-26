package com.example.barbershopmob.ui.booking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BookingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Запишитесь к своему любимому барберу онлайн"
    }
    val text: LiveData<String> = _text
}