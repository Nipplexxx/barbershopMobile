package com.example.barbershopmob.ui.profile

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.barbershopmob.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    private val user by lazy { FirebaseAuth.getInstance().currentUser }
    private val dbRef by lazy { FirebaseDatabase.getInstance().getReference("users").child(user!!.uid) }
    private val storageRef by lazy { FirebaseStorage.getInstance().getReference("avatars").child(user!!.uid) }
    private val scheduleRef by lazy {
        FirebaseDatabase.getInstance().getReference("barberSchedules").child(user!!.uid)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()

        // Настройка спиннеров
        val daysOfWeek = listOf(
            "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье"
        )
        val dayAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, daysOfWeek)
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.dayOfWeekSpinner.adapter = dayAdapter

        val timeSlots = listOf(
            "09:00-10:00", "10:00-11:00", "11:00-12:00", "12:00-13:00",
            "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00"
        )
        val timeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, timeSlots)
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.timeSlotSpinner.adapter = timeAdapter

        val pointsPercentOptions = listOf("5%", "10%", "15%")
        val percentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, pointsPercentOptions)
        percentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.pointsPercentSpinner.adapter = percentAdapter

        // Кнопка сохранить профиль (ФИО, телефон)
        binding.btnSaveProfile.setOnClickListener {
            val fullName = binding.editTextFullName.text.toString().trim()
            val phone = binding.editTextPhone.text.toString().trim()

            if (fullName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf("fullName" to fullName, "phone" to phone)
            dbRef.updateChildren(updates).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(requireContext(), "Профиль обновлён", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ошибка при сохранении", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Кнопка добавить расписание/услуги
        binding.btnAddSchedule.setOnClickListener {
            val selectedDay = binding.dayOfWeekSpinner.selectedItem.toString()
            val selectedTimeSlot = binding.timeSlotSpinner.selectedItem.toString()
            val services = binding.editServices.text.toString()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            if (services.isEmpty()) {
                Toast.makeText(requireContext(), "Введите хотя бы одну услугу", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val priceText = binding.editServicePrice.text.toString().trim()
            if (priceText.isEmpty()) {
                Toast.makeText(requireContext(), "Введите стоимость услуги", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val price = priceText.toDoubleOrNull()
            if (price == null || price <= 0) {
                Toast.makeText(requireContext(), "Введите корректную стоимость услуги", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val pointsPercentStr = binding.pointsPercentSpinner.selectedItem.toString()
            val pointsPercent = pointsPercentStr.removeSuffix("%").toIntOrNull() ?: 0

            // Структура в Firebase:
            // barberSchedules / userId / день / время / {
            //    services: [...],
            //    price: 123.45,
            //    pointsPercent: 10
            // }
            val scheduleData = mapOf(
                "services" to services,
                "price" to price,
                "pointsPercent" to pointsPercent
            )

            scheduleRef.child(selectedDay).child(selectedTimeSlot).setValue(scheduleData)
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(requireContext(), "Часы работы и услуги добавлены", Toast.LENGTH_SHORT).show()
                        binding.editServices.text.clear()
                        binding.editServicePrice.text.clear()
                        binding.pointsPercentSpinner.setSelection(0)
                    } else {
                        Toast.makeText(requireContext(), "Ошибка при добавлении часов", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Выбор аватара
        binding.avatarImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            uploadAvatar()
        }
    }

    private fun uploadAvatar() {
        imageUri?.let { uri ->
            storageRef.putFile(uri).addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    dbRef.child("avatarUrl").setValue(downloadUri.toString())
                    Glide.with(this).load(downloadUri).into(binding.avatarImage)
                    Toast.makeText(requireContext(), "Аватар загружен", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserData() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.editTextFullName.setText(snapshot.child("fullName").getValue(String::class.java))
                binding.editTextPhone.setText(snapshot.child("phone").getValue(String::class.java))

                val avatarUrl = snapshot.child("avatarUrl").getValue(String::class.java)
                if (!avatarUrl.isNullOrEmpty()) {
                    Glide.with(this@ProfileFragment).load(avatarUrl).into(binding.avatarImage)
                }

                val points = snapshot.child("points").getValue(Int::class.java) ?: 0
                binding.textPoints.text = "Баллы: $points"

                val role = snapshot.child("role").getValue(String::class.java)
                if (role == "barber") {
                    binding.scheduleLayout.visibility = View.VISIBLE
                    binding.daysContainer.visibility = View.VISIBLE
                } else {
                    binding.scheduleLayout.visibility = View.GONE
                    binding.daysContainer.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}