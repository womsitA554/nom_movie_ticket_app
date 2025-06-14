package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import Food
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.databinding.FoodItemBinding
import java.text.NumberFormat
import java.util.Locale

class FoodAdapter (private val listFood: List<Food>, private val isGrid: Boolean = false) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {
    var onClickItem: (Food, Int) -> Unit = { _, _ -> }

    inner class FoodViewHolder(private val binding: FoodItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onClickItem.invoke(listFood[adapterPosition], adapterPosition)
            }
        }
        fun onBind(food: Food){
            binding.tvTitleFood.text = food.title
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            val foodPrice = food.price.toInt()
            binding.tvPrice.text = formatter.format(foodPrice) + "đ"
            Glide.with(binding.root.context).load(food.picUrl).into(binding.picFood)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodAdapter.FoodViewHolder {
        val binding = FoodItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val layoutParams = binding.root.layoutParams
        if (!isGrid) {
            layoutParams.width = parent.context.resources.getDimensionPixelSize(R.dimen.item_width)
            layoutParams.height =
                parent.context.resources.getDimensionPixelSize(R.dimen.item_food_height_315)
        } else {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height =
                parent.context.resources.getDimensionPixelSize(R.dimen.item_height_440)
        }
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodAdapter.FoodViewHolder, position: Int) {
        holder.onBind(listFood[position])
    }

    override fun getItemCount(): Int {
        return listFood.size
    }
}