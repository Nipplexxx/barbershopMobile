package com.example.barbershopmob.ui.services

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.barbershopmob.models.ServiceItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ServicesViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Все услуги из базы
    private var allServices: List<ServiceItem> = emptyList()

    // LiveData для отображения отфильтрованного списка
    private val _services = MutableLiveData<List<ServiceItem>>()
    val services: LiveData<List<ServiceItem>> = _services

    // LiveData для сообщений (например, ошибки или уведомления)
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // Слушатель изменений в Firestore (для realtime обновлений)
    private var listenerRegistration: ListenerRegistration? = null

    init {
        loadServices()
    }

    private fun loadServices() {
        // Подписываемся на изменения коллекции "services"
        listenerRegistration = firestore.collection("services")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _message.value = "Ошибка загрузки услуг: ${error.message}"
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    allServices = snapshot.documents.map { doc ->
                        ServiceItem(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            dayOfWeek = doc.getString("dayOfWeek") ?: "",
                            time = doc.getString("time") ?: "",
                            barberId = doc.getString("barberId") ?: "",
                            barberName = doc.getString("barberName") ?: ""
                        )
                    }
                    _services.value = allServices
                }
            }
    }

    fun filterServices(query: String) {
        if (query.isBlank()) {
            _services.value = allServices
            return
        }
        val filtered = allServices.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.barberName.contains(query, ignoreCase = true)
        }
        _services.value = filtered
    }

    fun deleteService(serviceId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        firestore.collection("services")
            .document(serviceId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
                _message.value = "Услуга удалена"
            }
            .addOnFailureListener { e ->
                onFailure(e.message ?: "Ошибка при удалении")
            }
    }

    override fun onCleared() {
        super.onCleared()
        // Отписываемся от обновлений Firestore при уничтожении ViewModel
        listenerRegistration?.remove()
    }
}