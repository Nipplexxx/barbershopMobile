package com.example.barbershopmob.ui.barbers

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barbershopmob.data.model.User
import com.example.barbershopmob.databinding.ItemBarberBinding

class BarberAdapter(private val barbers: List<User>) :
    RecyclerView.Adapter<BarberAdapter.BarberViewHolder>() {

    inner class BarberViewHolder(private val binding: ItemBarberBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barber: User) {
            binding.textViewFullName.text = barber.fullName
            binding.textViewPhone.text = barber.phone
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarberViewHolder {
        val binding = ItemBarberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BarberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarberViewHolder, position: Int) {
        holder.bind(barbers[position])
    }

    override fun getItemCount(): Int = barbers.size
}