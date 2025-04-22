package com.example.kotlin_customer_nom_movie_ticket.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.databinding.FoodOrderItemBinding

class FoodOrderAdapter(private val list: MutableList<Cart>) : RecyclerView.Adapter<FoodOrderAdapter.viewHolder>() {
    var onClickDeleteItem : (Cart, Int) -> Unit = { _, _ -> }
    var onClickAddItem : (Cart, Int) -> Unit = { _, _ -> }
    var onClickRemoveItem : (Cart, Int) -> Unit = { _, _ -> }
    inner class viewHolder(private val binding: FoodOrderItemBinding) : RecyclerView.ViewHolder(binding.root){
        init {
            binding.btnEdit.setOnClickListener {
                onClickDeleteItem.invoke(list[adapterPosition], adapterPosition)
            }
            binding.btnAdd.setOnClickListener {
                onClickAddItem.invoke(list[adapterPosition], adapterPosition)
            }
            binding.btnRemove.setOnClickListener {
                onClickRemoveItem.invoke(list[adapterPosition], adapterPosition)
            }
        }
        fun onBind(order: Cart){
            val options = RequestOptions().transform(CenterCrop())
            order.picUrl?.let { url ->
                Glide.with(binding.img.context)
                    .load(url)
                    .apply(options)
                    .into(binding.img)
            }
            binding.tvTitle.text = order.title
            binding.tvQuantity.text = order.quantity.toString()
            binding.tvTotalPrice.text ="$" + order.price
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): viewHolder {
        val binding = FoodOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolder(binding)
    }

    override fun onBindViewHolder(holder: viewHolder, position: Int) {
        holder.onBind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateCarts(newCarts: List<Cart>) {
        val diffCallback = CartDiffCallback(list, newCarts)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        list.clear()
        list.addAll(newCarts)
        diffResult.dispatchUpdatesTo(this)
    }

    class CartDiffCallback(
        private val oldList: List<Cart>,
        private val newList: List<Cart>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].itemId == newList[newItemPosition].itemId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}