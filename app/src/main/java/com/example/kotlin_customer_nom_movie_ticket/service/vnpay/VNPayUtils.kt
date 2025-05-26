package com.example.kotlin_customer_nom_movie_ticket.service.vnpay

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import android.util.Log

object VNPayUtils {
    fun createPaymentUrl(
        amount: Long,
        orderInfo: String,
        orderId: String,
        ipAddr: String,
        returnUrl: String = VNPayConfig.RETURN_URL_TICKET
    ): String {
        val vnpParams = sortedMapOf<String, String>()
        vnpParams["vnp_Version"] = VNPayConfig.VERSION
        vnpParams["vnp_Command"] = VNPayConfig.COMMAND
        vnpParams["vnp_TmnCode"] = VNPayConfig.TMN_CODE
        vnpParams["vnp_Amount"] = (amount * 100).toString()
        vnpParams["vnp_CreateDate"] = getCurrentDate()
        vnpParams["vnp_CurrCode"] = VNPayConfig.CURRENCY_CODE
        vnpParams["vnp_IpAddr"] = ipAddr
        vnpParams["vnp_Locale"] = VNPayConfig.LOCALE
        vnpParams["vnp_OrderInfo"] = orderInfo
        vnpParams["vnp_OrderType"] = "other"
        vnpParams["vnp_ReturnUrl"] = returnUrl
        vnpParams["vnp_TxnRef"] = orderId

        // Tạo chuỗi hash data
        val hashData = vnpParams.map { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8.name())}" }
            .joinToString("&")

        // Tạo chữ ký bằng HMACSHA512
        val signature = createHmacSha512Signature(hashData, VNPayConfig.HASH_SECRET)

        // Tạo URL thanh toán
        val queryString = vnpParams.map { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8.name())}" }
            .joinToString("&") + "&vnp_SecureHash=$signature"

        Log.d("VNPAY", "Hash Data: $hashData")
        Log.d("VNPAY", "Signature: $signature")
        Log.d("VNPAY", "Final URL: ${VNPayConfig.PAYMENT_URL}?$queryString")

        return "${VNPayConfig.PAYMENT_URL}?$queryString"
    }

    fun verifyReturnUrl(url: String): Pair<Boolean, String?> {
        try {
            val uri = Uri.parse(url)
            val vnpParams = sortedMapOf<String, String>()
            uri.queryParameterNames.forEach { key ->
                vnpParams[key] = uri.getQueryParameter(key) ?: ""
            }

            val secureHash = vnpParams["vnp_SecureHash"] ?: return Pair(false, null)
            vnpParams.remove("vnp_SecureHash")
            vnpParams.remove("vnp_SecureHashType")

            // Tạo chuỗi hash data
            val hashData = vnpParams.map { "${it.key}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8.name())}" }
                .joinToString("&")

            // Tạo chữ ký bằng HMACSHA512
            val calculatedSignature = createHmacSha512Signature(hashData, VNPayConfig.HASH_SECRET)

            Log.d("VNPAY", "Verify Hash Data: $hashData")
            Log.d("VNPAY", "Verify Secure Hash: $secureHash")
            Log.d("VNPAY", "Verify Calculated Signature: $calculatedSignature")

            return Pair(secureHash.equals(calculatedSignature, ignoreCase = true), vnpParams["vnp_ResponseCode"])
        } catch (e: Exception) {
            Log.e("VNPAY", "Failed to verify return URL: ${e.message}")
            return Pair(false, null)
        }
    }

    private fun createHmacSha512Signature(data: String, secretKey: String): String {
        try {
            val mac = Mac.getInstance("HmacSHA512")
            val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA512")
            mac.init(keySpec)
            val hash = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e("VNPAY", "Failed to create HMACSHA512 signature: ${e.message}")
            throw RuntimeException("Failed to create HMACSHA512 signature: ${e.message}", e)
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        return sdf.format(Date())
    }

    fun getOrderIdFromReturnUrl(returnUrl: String): String {
        val uri = Uri.parse(returnUrl)
        return uri.getQueryParameter("vnp_TxnRef") ?: ""
    }
}