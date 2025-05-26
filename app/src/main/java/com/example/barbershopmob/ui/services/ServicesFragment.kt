package com.example.barbershopmob.ui.services

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barbershopmob.databinding.FragmentServicesBinding
import com.example.barbershopmob.models.ServiceItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ServicesFragment : Fragment() {

    private var _binding: FragmentServicesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ServicesAdapter
    private var allServices = listOf<ServiceItem>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val usersRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentServicesBinding.inflate(inflater, container, false)

        adapter = ServicesAdapter(currentUserId, allServices) { service ->
            deleteService(service)
        }
        binding.recyclerViewServices.adapter = adapter
        binding.recyclerViewServices.layoutManager = LinearLayoutManager(requireContext())

        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterServices(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadServicesFromDatabase()

        return binding.root
    }

    private fun loadServicesFromDatabase() {
        val schedulesRef = FirebaseDatabase.getInstance().getReference("barberSchedules")

        schedulesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val servicesTemp = mutableListOf<ServiceItem>()
                val barberScheduleSnapshots = snapshot.children.toList()
                var pendingCount = barberScheduleSnapshots.size

                if (pendingCount == 0) {
                    allServices = servicesTemp
                    adapter.updateList(allServices)
                    return
                }

                for (barberScheduleSnapshot in barberScheduleSnapshots) {
                    val barberId = barberScheduleSnapshot.key ?: continue

                    usersRef.child(barberId).child("fullName")
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(barberSnapshot: DataSnapshot) {
                                val barberName = barberSnapshot.getValue(String::class.java) ?: "Без имени"

                                for (daySnapshot in barberScheduleSnapshot.children) {
                                    val dayOfWeek = daySnapshot.key ?: continue

                                    for (timeSnapshot in daySnapshot.children) {
                                        val time = timeSnapshot.key ?: continue

                                        val servicesList = timeSnapshot.child("services").children.mapNotNull {
                                            it.getValue(String::class.java)
                                        }

                                        val price = timeSnapshot.child("price").getValue(Double::class.java) ?: 0.0
                                        val pointsPercent = timeSnapshot.child("pointsPercent").getValue(Int::class.java) ?: 0

                                        for (serviceName in servicesList) {
                                            servicesTemp.add(
                                                ServiceItem(
                                                    id = "$barberId-$dayOfWeek-$time-$serviceName",
                                                    name = serviceName,
                                                    dayOfWeek = dayOfWeek,
                                                    time = time,
                                                    barberId = barberId,
                                                    barberName = barberName,
                                                    price = price,
                                                    pointsPercent = pointsPercent
                                                )
                                            )
                                        }
                                    }
                                }

                                pendingCount--
                                if (pendingCount == 0) {
                                    allServices = servicesTemp
                                    adapter.updateList(allServices)
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                pendingCount--
                                if (pendingCount == 0) {
                                    allServices = servicesTemp
                                    adapter.updateList(allServices)
                                }
                            }
                        })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Ошибка загрузки услуг", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun filterServices(query: String) {
        val filtered = allServices.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.barberName.contains(query, ignoreCase = true)
        }
        adapter.updateList(filtered)
    }

    private fun deleteService(service: ServiceItem) {
        val servicePath = "barberSchedules/${service.barberId}/${service.dayOfWeek}/${service.time}/services"
        val serviceRef = FirebaseDatabase.getInstance().getReference(servicePath)

        serviceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updatedServices = snapshot.children.mapNotNull { it.getValue(String::class.java) }.toMutableList()
                if (updatedServices.remove(service.name)) {
                    serviceRef.setValue(updatedServices).addOnSuccessListener {
                        allServices = allServices.filter { it.id != service.id }
                        adapter.updateList(allServices)
                        Toast.makeText(requireContext(), "Услуга удалена", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Ошибка при удалении", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Услуга не найдена", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Ошибка чтения базы", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}