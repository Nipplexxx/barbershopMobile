package com.example.barbershopmob.ui.booking

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.example.barbershopmob.databinding.FragmentBookingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

data class TimeSlot(
    val time: String,
    val barberName: String,
    val price: Int,
    val points: Int,
    val serviceName: String,
    val barberId: String? = null
)

class BookingFragment : Fragment() {

    private var _binding: FragmentBookingBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseDatabase.getInstance().reference
    private val clientId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val daysOfWeek = listOf(
        "Понедельник",
        "Вторник",
        "Среда",
        "Четверг",
        "Пятница",
        "Суббота",
        "Воскресенье"
    )

    private var availableTimeSlots: List<TimeSlot> = emptyList()

    // Словарь barberId -> ФИО барбера
    private val barberNamesMap = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookingBinding.inflate(inflater, container, false)

        setupDaySpinner()

        loadBarberNames {
            binding.spinnerDay.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?, view: View?, position: Int, id: Long
                ) {
                    val selectedDay = daysOfWeek[position]
                    loadTimeSlotsForDay(selectedDay)
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }

        binding.spinnerTime.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                showPointsForSelectedTime()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.buttonBook.setOnClickListener {
            val selectedDay = binding.spinnerDay.selectedItem.toString()
            val selectedSlot = binding.spinnerTime.selectedItem as? TimeSlot
            if (selectedSlot != null) {
                bookTime(selectedDay, selectedSlot)
            } else {
                binding.textPoints.text = "Пожалуйста, выберите время"
            }
        }

        return binding.root
    }

    private fun setupDaySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDay.adapter = adapter
    }

    private fun loadBarberNames(onComplete: () -> Unit) {
        database.child("barbers").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                barberNamesMap.clear()
                for (barberSnapshot in snapshot.children) {
                    val id = barberSnapshot.key ?: continue
                    val name = barberSnapshot.child("fullName").getValue(String::class.java) ?: id
                    barberNamesMap[id] = name
                }
                onComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.textPoints.text = "Ошибка загрузки барберов: ${error.message}"
                onComplete()
            }
        })
    }

    private fun loadTimeSlotsForDay(dayName: String) {
        val slots = mutableListOf<TimeSlot>()

        database.child("barberSchedules").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                slots.clear()

                for (barberSnapshot in snapshot.children) {
                    val barberId = barberSnapshot.key ?: continue

                    val daySchedule = barberSnapshot.child(dayName)
                    if (!daySchedule.exists()) continue

                    val barberName = barberNamesMap[barberId] ?: barberId

                    for (timeSlotSnapshot in daySchedule.children) {
                        val timeRange = timeSlotSnapshot.key ?: continue

                        val price = timeSlotSnapshot.child("price").getValue(Int::class.java) ?: 0
                        val pointsPercent = timeSlotSnapshot.child("pointsPercent").getValue(Int::class.java) ?: 0
                        val servicesList = timeSlotSnapshot.child("services").children.mapNotNull { it.getValue(String::class.java) }

                        for (service in servicesList) {
                            slots.add(TimeSlot(
                                time = timeRange,
                                barberName = barberName,
                                price = price,
                                points = pointsPercent,
                                serviceName = service,
                                barberId = barberId
                            ))
                        }
                    }
                }

                availableTimeSlots = slots
                updateTimeSpinner()
            }

            override fun onCancelled(error: DatabaseError) {
                binding.textPoints.text = "Ошибка загрузки времени: ${error.message}"
            }
        })
    }

    private fun updateTimeSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, availableTimeSlots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTime.adapter = adapter

        if (availableTimeSlots.isNotEmpty()) {
            binding.spinnerTime.setSelection(0)
            showPointsForSelectedTime()
        } else {
            binding.textPoints.text = "Нет доступных слотов на выбранный день"
        }
    }

    private fun showPointsForSelectedTime() {
        val slot = binding.spinnerTime.selectedItem as? TimeSlot
        binding.textPoints.text = slot?.let { "При записи получите ${it.points} баллов" } ?: ""
    }

    private fun bookTime(day: String, slot: TimeSlot) {
        val bookingKey = "${day}_${slot.time}"
        val barberIdentifier = slot.barberId ?: slot.barberName
        val bookingRef = database.child("bookings").child(barberIdentifier).child(bookingKey)

        bookingRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                binding.textPoints.text = "Выбранное время уже занято"
            } else {
                val bookingData = mapOf(
                    "clientId" to clientId,
                    "serviceName" to slot.serviceName,
                    "price" to slot.price,
                    "points" to slot.points,
                    "day" to day,
                    "time" to slot.time,
                    "barberName" to barberIdentifier
                )
                bookingRef.setValue(bookingData).addOnSuccessListener {
                    binding.textPoints.text = "Вы успешно записались! Получите ${slot.points} баллов"
                }
            }
        }.addOnFailureListener {
            binding.textPoints.text = "Ошибка при записи. Попробуйте позже."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}