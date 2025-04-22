//package com.example.kotlin_customer_nom_movie_ticket.ui.adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.example.kotlin_customer_nom_movie_ticket.R
//import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.PaymentMethodData
//
//class CardAdapter(
//    private val cards: List<PaymentMethodData>,
//    private val onCardSelected: (PaymentMethodData) -> Unit
//) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
//        return CardViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
//        val card = cards[position]
//        holder.bind(card)
//    }
//
//    override fun getItemCount(): Int = cards.size
//
//    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val cardInfo: TextView = itemView.findViewById(R.id.cardInfo)
//
//        fun bind(card: PaymentMethodData) {
//            cardInfo.text = "${card.brand} **** ${card.last4}"
//            itemView.setOnClickListener { onCardSelected(card) }
//        }
//    }
//}