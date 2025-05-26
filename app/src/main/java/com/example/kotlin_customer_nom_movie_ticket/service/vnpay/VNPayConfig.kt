package com.example.kotlin_customer_nom_movie_ticket.service.vnpay

object VNPayConfig {
    const val TMN_CODE = "C336UKSS"
    const val HASH_SECRET = "A6ZQOLVUI5NNIKFX6OOJGQFB9JT3PB8Z"
    const val PAYMENT_URL = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
    const val RETURN_URL_TICKET = "vnpay://ticket/return"
    const val RETURN_URL_FOOD = "vnpay://food/return"
    const val CURRENCY_CODE = "VND"
    const val LOCALE = "vi"
    const val COMMAND = "pay"
    const val VERSION = "2.1.0"
} 