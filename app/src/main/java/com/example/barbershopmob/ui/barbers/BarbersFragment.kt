package com.example.barbershopmob.ui.barbers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barbershopmob.data.model.User
import com.example.barbershopmob.databinding.FragmentBarbersBinding
import com.google.firebase.database.*

class BarbersFragment : Fragment() {

    private var _binding: FragmentBarbersBinding? = null
    private val binding get() = _binding!!

    private val dbRef: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().getReference("users")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarbersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewBarbers.layoutManager = LinearLayoutManager(requireContext())
        loadBarbers()
    }

    private fun loadBarbers() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val barberList = mutableListOf<User>()
                for (child in snapshot.children) {
                    val role = child.child("role").getValue(String::class.java)
                    if (role == "barber") {
                        val uid = child.key ?: continue
                        val fullName = child.child("fullName").getValue(String::class.java) ?: "Без имени"
                        val phone = child.child("phone").getValue(String::class.java) ?: "Телефон не указан"
                        barberList.add(User(
                            userId = uid,
                            fullName = fullName,
                            phone = phone,
                            role = role
                        ))
                    }
                }
                binding.recyclerViewBarbers.adapter = BarberAdapter(barberList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Здесь можно обработать ошибку, например, показать Toast
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}