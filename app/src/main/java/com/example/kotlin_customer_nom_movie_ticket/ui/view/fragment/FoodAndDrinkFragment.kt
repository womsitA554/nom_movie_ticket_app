package com.example.kotlin_customer_nom_movie_ticket.ui.view.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.kotlin_customer_nom_movie_ticket.R
import com.example.kotlin_customer_nom_movie_ticket.data.model.Cart
import com.example.kotlin_customer_nom_movie_ticket.databinding.FragmentFoodAndDrinkBinding
import com.example.kotlin_customer_nom_movie_ticket.helper.HorizontalSpaceItemDecoration
import com.example.kotlin_customer_nom_movie_ticket.ui.adapter.FoodAdapter
import com.example.kotlin_customer_nom_movie_ticket.ui.view.activity.FoodPaymentDetailActivity
import com.example.kotlin_customer_nom_movie_ticket.util.CartManager
import com.example.kotlin_customer_nom_movie_ticket.util.SessionManager
import com.example.kotlin_customer_nom_movie_ticket.viewmodel.FoodViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.NumberFormat
import java.util.Locale

class FoodAndDrinkFragment : Fragment() {
    private var _binding: FragmentFoodAndDrinkBinding? = null
    private val binding get() = _binding!!
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var foodViewModel: FoodViewModel
    private lateinit var userId: String
    private var quantity = 0
    private lateinit var cartManager: CartManager
    private lateinit var broadcastReceiver: BroadcastReceiver

    // Biến để theo dõi trạng thái load của từng danh mục
    private var isFoodLoaded = false
    private var isDrinkLoaded = false
    private var isComboLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoodAndDrinkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo UI ban đầu: hiển thị progressBar, ẩn scrollView
        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.playAnimation() // Bắt đầu animation
        binding.scrollView.visibility = View.GONE

        userId = SessionManager.getUserId(requireContext()) ?: run {
            requireActivity().finish()
            return
        }
        foodViewModel = ViewModelProvider(this)[FoodViewModel::class.java]
        cartManager = CartManager(requireContext())

        updateCartUI()
        setupRecycleView()

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action in listOf("PAYMENT_SUCCESS", "UPDATE_CART", "CART_UPDATED")) {
                    updateCartUI()
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction("PAYMENT_SUCCESS")
            addAction("UPDATE_CART")
            addAction("CART_UPDATED")
        }
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(broadcastReceiver, intentFilter)

        foodViewModel.fetchAllFood()
        foodViewModel.fetchAllDrink()
        foodViewModel.fetchAllCombo()

        // Thiết lập observers
        setupObservers()
    }

    private fun setupObservers() {
        foodViewModel.food.observe(viewLifecycleOwner) { foodList ->
            isFoodLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvFood.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                Log.d("FoodAndDrinkFragment", "Food clicked: ${food.title}, itemId: ${food.itemId}, isAvailable: ${food.isAvailable}")
                showBottomSheetDialog(
                    food.itemId,
                    food.picUrl,
                    food.title,
                    food.description,
                    food.price,
                    food.isAvailable!!
                )
            }
            checkAllDataLoaded()
        }

        foodViewModel.drink.observe(viewLifecycleOwner) { foodList ->
            isDrinkLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvDrink.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                Log.d("FoodAndDrinkFragment", "Drink clicked: ${food.title}, itemId: ${food.itemId}, isAvailable: ${food.isAvailable}")
                showBottomSheetDialog(
                    food.itemId,
                    food.picUrl,
                    food.title,
                    food.description,
                    food.price,
                    food.isAvailable!!
                )
            }
            checkAllDataLoaded()
        }

        foodViewModel.combo.observe(viewLifecycleOwner) { foodList ->
            isComboLoaded = true
            foodAdapter = FoodAdapter(foodList, false)
            binding.rcvCombo.adapter = foodAdapter
            foodAdapter.onClickItem = { food, _ ->
                Log.d("FoodAndDrinkFragment", "Combo clicked: ${food.title}, itemId: ${food.itemId}, isAvailable: ${food.isAvailable}")
                showBottomSheetDialog(
                    food.itemId,
                    food.picUrl,
                    food.title,
                    food.description,
                    food.price,
                    food.isAvailable!!
                )
            }
            checkAllDataLoaded()
        }
    }

    private fun checkAllDataLoaded() {
        if (isFoodLoaded && isDrinkLoaded && isComboLoaded) {
            // Tất cả dữ liệu đã load, cập nhật UI
            stopAnimation()
            binding.scrollView.visibility = View.VISIBLE
            Log.d("FoodAndDrinkFragment", "All data loaded, showing scrollView")
        }
    }

    private fun stopAnimation() {
        binding.progressBar.cancelAnimation() // Dừng animation
        binding.progressBar.visibility = View.GONE // Ẩn progressBar
    }

    private fun updateCartUI() {
        val cartItems = cartManager.getCart(userId)
        val totalQuantity = cartItems.sumOf { it.quantity ?: 0 }
        binding.tvQuantity.text = totalQuantity.toString()
        if (totalQuantity > 0) {
            binding.btnCart.visibility = View.VISIBLE
            binding.btnCart.setOnClickListener {
                val intent = Intent(requireContext(), FoodPaymentDetailActivity::class.java)
                startActivity(intent)
                activity?.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        } else {
            binding.btnCart.visibility = View.GONE
        }

        cartItems.forEach {
            Log.d("CartItem", "Item: ${it.title}, Quantity: ${it.quantity}, Price: ${it.price}")
        }
    }

    private fun setupRecycleView() {
        val spaceInPixels = resources.getDimensionPixelSize(R.dimen.item_spacing)

        binding.rcvFood.setHasFixedSize(true)
        binding.rcvFood.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvFood.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))

        binding.rcvDrink.setHasFixedSize(true)
        binding.rcvDrink.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvDrink.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))

        binding.rcvCombo.setHasFixedSize(true)
        binding.rcvCombo.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rcvCombo.addItemDecoration(HorizontalSpaceItemDecoration(spaceInPixels))
    }

    private fun showBottomSheetDialog(
        itemId: String,
        img: String,
        title: String,
        description: String,
        price: Double,
        isAvailable: Boolean
    ) {
        Log.d("FoodAndDrinkFragment", "showBottomSheetDialog: $itemId, $title, isAvailable: $isAvailable")
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(R.layout.bottom_sheet_food)

        val picFood = dialog.findViewById<ImageView>(R.id.picFood)
        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        val tvDescription = dialog.findViewById<TextView>(R.id.tvDescription)
        val btnRemove = dialog.findViewById<ImageView>(R.id.btnRemove)
        val tvQuantity = dialog.findViewById<TextView>(R.id.tvQuantity)
        val btnAdd = dialog.findViewById<ImageView>(R.id.btnAdd)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        val btnContinue = dialog.findViewById<Button>(R.id.btnContinue)

        quantity = 1
        Glide.with(dialog.context).load(img).into(picFood!!)
        tvTitle?.text = title
        tvDescription?.text = description
        tvQuantity?.text = quantity.toString()

        if (isAvailable) {
            // Food is available, enable button and set price
            val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
            btnContinue?.text = "Thêm vào giỏ hàng - ${formatter.format((price * quantity).toInt())}đ"
            btnContinue?.isEnabled = true
            btnAdd?.isEnabled = true
            btnRemove?.isEnabled = true
            btnAdd?.setOnClickListener {
                quantity++
                tvQuantity?.text = quantity.toString()
                btnContinue?.text = "Thêm vào giỏ hàng - ${formatter.format((price * quantity).toInt())}đ"
            }
            btnRemove?.setOnClickListener {
                if (quantity > 1) {
                    quantity--
                    tvQuantity?.text = quantity.toString()
                    btnContinue?.text = "Thêm vào giỏ hàng - ${formatter.format((price * quantity).toInt())}đ"
                }
            }
            btnContinue?.setOnClickListener {
                val cartItem = Cart(
                    itemId = itemId,
                    title = title,
                    picUrl = img,
                    price = price * quantity,
                    quantity = quantity
                )
                cartManager.addItemToCart(userId, cartItem, quantity)
                updateCartUI()
                dialog.dismiss()
            }
        } else {
            // Food is not available, disable button and show out-of-stock message
            btnContinue?.text = "Tạm thời hết hàng"
            btnContinue?.isEnabled = false
            btnAdd?.isEnabled = false
            btnRemove?.isEnabled = false
        }

        btnCancel?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

        val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
            behavior.isHideable = true

            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dialog.dismiss()
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset < 0.01) {
                        dialog.dismiss()
                    }
                    Log.d("BottomSheet", "Slide offset: $slideOffset")
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAnimation() // Dừng và ẩn progressBar khi fragment bị hủy
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(broadcastReceiver)
        _binding = null
    }
}