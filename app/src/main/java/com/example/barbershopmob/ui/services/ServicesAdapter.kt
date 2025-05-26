package com.example.barbershopmob.ui.services

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.barbershopmob.R
import com.example.barbershopmob.models.ServiceItem

class ServicesAdapter(
    private val currentUserId: String,
    private var servicesList: List<ServiceItem>,
    private val onDeleteClick: (ServiceItem) -> Unit
) : RecyclerView.Adapter<ServicesAdapter.ServiceViewHolder>() {

    inner class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textServiceName)
        val textDayTime: TextView = itemView.findViewById(R.id.textDayTime)
        val textBarber: TextView = itemView.findViewById(R.id.textBarberName)
        val textPrice: TextView = itemView.findViewById(R.id.textServicePrice)
        val textPointsPercent: TextView = itemView.findViewById(R.id.textPointsPercent)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteService)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_service, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        val service = servicesList[position]

        holder.textName.text = service.name
        holder.textDayTime.text = "${service.dayOfWeek}, ${service.time}"
        holder.textBarber.text = "Барбер: ${service.barberName}"
        holder.textPrice.text = "Стоимость: ${service.price} ₽"
        holder.textPointsPercent.text = "Баллы: ${service.pointsPercent}%"

        if (service.barberId == currentUserId) {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnDelete.setOnClickListener {
                onDeleteClick(service)
            }
        } else {
            holder.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = servicesList.size

    fun updateList(newList: List<ServiceItem>) {
        servicesList = newList
        notifyDataSetChanged()
    }
}