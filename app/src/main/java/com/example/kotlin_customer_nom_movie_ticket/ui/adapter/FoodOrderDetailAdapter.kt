package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.data.model.FoodItem
import com.example.kotlin_customer_nom_movie_ticket.databinding.FoodOrderDetailItemBinding
import com.example.kotlin_customer_nom_movie_ticket.databinding.FoodOrderItemBinding
import java.text.NumberFormat
import java.util.Locale

class FoodOrderDetailAdapter(private val list: List<FoodItem>) : RecyclerView.Adapter<FoodOrderDetailAdapter.viewHolder>() {
    class viewHolder(private val binding: FoodOrderDetailItemBinding) : RecyclerView.ViewHolder(binding.root){
        fun onBind(foodItem: FoodItem){
            val options = RequestOptions().transform(CenterCrop())
            foodItem.picUrl.let { url ->
                Glide.with(binding.img.context)
                    .load(url)
                    .apply(options)
                    .into(binding.img)
            }
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))

            binding.tvTitle.text = foodItem.title
            binding.tvQuantity.text = "x" + foodItem.quantity.toString()
            binding.tvTotalPrice.text = formatter.format(foodItem.price?.toInt()) + "đ"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val binding = FoodOrderDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolder(binding)
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}